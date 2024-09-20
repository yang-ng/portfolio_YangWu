#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <signal.h>
#include <termios.h>
#include <readline/readline.h>
#include <fcntl.h>
#include <dirent.h>
#include <sys/stat.h>
#include <time.h>
#include <pwd.h>
#include <grp.h>
#include <sys/ioctl.h>

#define INPUT_LENGTH 255
#define BUFFER_SIZE 1024
#define PROMPT "shell> "
#define DELIMITERS " ;\t\n"
#define TRUE 1
#define EXITED 0
#define KILLED 1
#define STOPPED 2
#define CONTINUED 3

// global variables
struct Job* job_head = NULL; // head of list of jobs
struct Job* job_tail = NULL; // tail of list of jobs
struct DeferredJob* deferred_head = NULL; // head of list of deferred jobs
int num_jobs = 0;

enum JobStatus {
    running,
    suspended,
};

struct Job {
    int jid; // job ID
    pid_t pid;
    int isBG;
    char command[INPUT_LENGTH];
    enum JobStatus status;
    struct termios setting;
    struct Job* next;
    struct Job* prev; 
};

struct DeferredJob {
    pid_t pid;
    int flag;
    struct termios setting;
    struct DeferredJob* next;
};

void printJobList() { // used for command "jobs"
    struct Job* current = job_head;
    if (current == NULL) {
        printf("no jobs\n");
    } else {
        while (current != NULL) {
            printf("[%d]pid: %d ", current->jid, (int)current->pid);
            if (current->status == running) {
                printf("running     ");
            } else {
                printf("suspended     ");
            }
            printf("command: %s\n", current->command);
            current = current->next;
        }
    }
}

void shell_ignore_signals() {
    signal(SIGINT, SIG_IGN);
    signal(SIGTERM, SIG_IGN);
    signal(SIGTTIN, SIG_IGN);
    signal(SIGTTOU, SIG_IGN);
    signal(SIGTSTP, SIG_IGN);
    signal(SIGQUIT, SIG_IGN);
}

void child_handle_signals() {
    signal(SIGINT, SIG_DFL);
    signal(SIGTERM, SIG_DFL);
    signal(SIGTTIN, SIG_DFL);
    signal(SIGTTOU, SIG_DFL);
    signal(SIGTSTP, SIG_DFL);
    signal(SIGQUIT, SIG_DFL);
}

struct Job* newJob(pid_t pid, const char* command, int isBG) { 
    struct Job* new = malloc(sizeof(struct Job));
    if (new == NULL) {
        perror("Error creating job");
        exit(EXIT_FAILURE);
    }

    new->jid = ++num_jobs; 
    new->isBG = isBG;
    strncpy(new->command, command, sizeof(new->command) - 1);
    new->status = running;
    new->pid = pid;
    struct termios setting;
    if(tcgetattr(STDIN_FILENO, &setting)< 0) {
        perror("tcgetattr");
        exit(EXIT_FAILURE);
    }
    new->setting = setting;
    new->next = NULL;
    new->prev = NULL;
    return new;
}

void insertJob(pid_t pid, const char* command, int isBG) {
    struct Job* new = newJob(pid, command, isBG);
    if (job_head == NULL) {
        job_head = new;
        job_tail = new; 
    } else {
        job_tail->next = new;
        new->prev = job_tail;
        job_tail = new;
    }
}

void removeJob(pid_t pid) {
    struct Job* current = job_head;
    while (current != NULL && current->pid != pid) {
        current = current->next;
    }
    if (current == NULL) {
        printf("target job is not found\n");
        return;
    }

    if (current->prev == NULL) {
        job_head = current->next;
        if (job_head != NULL) {
            job_head->prev = NULL;
        }
    } else {
        current->prev->next = current->next;
        if (current->next != NULL) {
            current->next->prev = current->prev;
        }
    }

    if (current == job_tail) {
        job_tail = current->prev;
    }

    struct Job* update = current->next;
    while (update!= NULL){
        update->jid--;
        update = update->next;
    }
    num_jobs--;
    free(current);
}

void freeJobList() {
    struct Job* current = job_head;
    while (current != NULL) {
        struct Job* temp = current;
        current = current->next;
        free(temp);
    }
}

struct DeferredJob* newDeferredJob(pid_t pid, int flag) {
    struct DeferredJob* new = malloc(sizeof(struct DeferredJob));
    if (new == NULL) {
        perror("failed to malloc DeferredJob");
        exit(EXIT_FAILURE);
    }
    new->pid = pid;
    new->flag = flag;
    new->next = NULL;
    return new;
}

void insertDeferredJob(pid_t pid, int flag) {
    struct DeferredJob* new = newDeferredJob(pid, flag);
    struct termios setting;
    if(tcgetattr(STDIN_FILENO, &setting)< 0) {
        perror("tcgetattr");
        exit(EXIT_FAILURE);
    }
    new ->setting = setting;

    if (deferred_head == NULL) {
        deferred_head = new;
    } else {
        struct DeferredJob* current = deferred_head;
        while (current->next != NULL) {
            current = current->next;
        }
        current->next = new;
    }
}

struct DeferredJob* getFirstDeferredJob() { // get and remove it
    if (deferred_head == NULL) {
        return NULL;
    }
    struct DeferredJob* current = deferred_head;
    deferred_head = deferred_head->next;
    return current;
}

void freeDeferredList() {
    struct DeferredJob* current;
    while (deferred_head != NULL) {
        current = deferred_head;
        deferred_head = deferred_head->next;
        free(current);
    }
}

void sigchld_handler(int signum, siginfo_t *info, void *context){
    pid_t pid = info -> si_pid; 

   	if(info->si_code == CLD_EXITED){ // when child is terminated
        int status;
        waitpid(pid, &status, WNOHANG);
        insertDeferredJob(pid, EXITED);
    }
    if(info->si_code== CLD_KILLED){ // when child is killed
        int status;
        waitpid(pid, &status, WNOHANG);
        insertDeferredJob(pid, KILLED);
    }
    if(info->si_code == CLD_STOPPED){ // when child is stopped
        kill(pid, SIGSTOP);
        insertDeferredJob(pid, STOPPED);
   	}
    if(info->si_code == CLD_CONTINUED){ // when child is continued
        struct termios setting;
        insertDeferredJob(pid, CONTINUED);
	}
}

void updateJobList(){
	while(deferred_head != NULL){
		pid_t pid = deferred_head->pid ;
		int flag = deferred_head->flag;
		if (flag == EXITED || flag == KILLED){
            removeJob(pid);
        } else {
            struct Job* current = job_head;
            while (current != NULL && current->pid != pid) {
                current = current->next;
            }
            if (current == NULL) {
                printf("job not found\n");
                return;
            }
            if(flag == STOPPED){
                current->status = suspended;
                current->setting = deferred_head->setting;
            }
            if(flag == CONTINUED){
                current->status = running;
            }
        }
        struct DeferredJob* head = getFirstDeferredJob();
        free(head);
	}
}

void execute_command(char *command) {
    int runBG = 0;
    
    // built-in command: exit
    if (strcmp(command, "exit") == 0) {
        free(command);
        exit(0);
    }

    // parse command
    char *args[INPUT_LENGTH];
    int argc = 0; // number of arguments
    char *token = strtok(command, DELIMITERS);
    if (token == NULL) {
        return;
    }

    char commandText[INPUT_LENGTH];
    strcpy(commandText, token);

    while (token != NULL) {
        args[argc++] = token;
        token = strtok(NULL, DELIMITERS);

        if (token != NULL) {
            strcat(commandText, " ");
            strcat(commandText, token);
        }
    } 

    // check "&"
    size_t length = strlen(commandText);
    if (strcmp(commandText + length - 1, "&") == 0) {
        commandText[length - 1] = '\0';
    }

    args[argc++] = NULL;

    // built-in command: pwd
    if (strcmp(args[0], "pwd") == 0) {
        char cwd[BUFFER_SIZE]; // buffer
        if (getcwd(cwd, sizeof(cwd)) != NULL) {
            printf("%s\n", cwd);
        } else {
            perror("getcwd failed");
        }
        return;
    }
    
    // built-in command: mkdir
    if (strcmp(args[0], "mkdir") == 0) {
        if (argc < 2) {
            fprintf(stderr, "error: mkdir requires at least one directory name\n");
            return;
        }
        for (int i = 1; i < argc; i++) {
            if (mkdir(args[i], 0755) != 0) {
                perror("mkdir failed");
            }
        }
        return;
    }

    // built-in command: rmdir
    if (strcmp(args[0], "rmdir") == 0) {
        if (argc < 2) {
            fprintf(stderr, "error: rmdir requires at least one directory name\n");
            return;
        }
        for (int i = 1; i < argc; i++) {
            if (rmdir(args[i]) != 0) {
               // perror("rmdir failed");
            }
        }
        return;
    }

    // built-in command: rm
    if (strcmp(args[0], "rm") == 0) {
        if (argc < 2) {
            fprintf(stderr, "error: rm requires at least one file name\n");
            return;
        }
        for (int i = 1; i < argc; i++) {
            if (unlink(args[i]) != 0) {
               // perror("rm failed");
            }
        }
        return;
    }

    // built-in command: cat
    if (strcmp(args[0], "cat") == 0) {
        int fd_out = STDOUT_FILENO; // default, print to standard output
        int redirect = 0;

        // check redirection
        for (int i = 1; i < argc; i++) {
            if (strcmp(args[i], ">") == 0 && i < argc - 1) {
                fd_out = open(args[i + 1], O_WRONLY | O_CREAT | O_TRUNC, 0644);
                if (fd_out == -1) {
                    perror("failed to open output file");
                    return;
                }
                redirect = 1;
                args[i] = NULL;
                argc = i;
                break;
            }
        }

        if (argc == 1) {  // no files given, read from standard input
            char buffer[BUFFER_SIZE];
            ssize_t bytes_read;
            while ((bytes_read = read(STDIN_FILENO, buffer, BUFFER_SIZE)) > 0) {
                if (write(fd_out, buffer, bytes_read) != bytes_read) {
                    perror("cat failed to write to stdout");
                    break;
                }
            }
            if (bytes_read == -1) {
                perror("cat failed to read from stdin");
            }
        } else { // files specified
            char buffer[BUFFER_SIZE];
            for (int j = 1; j < argc; j++) {
                if (args[j] == NULL) break;
                int fd = open(args[j], O_RDONLY);
                if (fd == -1) {
                    perror("cat failed to open file");
                    continue;
                }
                ssize_t bytes_read;
                while ((bytes_read = read(fd, buffer, BUFFER_SIZE)) > 0) {
                    if (write(fd_out, buffer, bytes_read) != bytes_read) {
                        perror("cat failed to write to output");
                        break;
                    }
                }
                if (bytes_read == -1) {
                    perror("cat failed to read file");
                }
                close(fd);
            }
            if (!redirect) {
                fprintf(stdout, "\n");
            }
        }
        if (redirect) {
            close(fd_out);
        }
        return;
    }

    // built-in command: ls
    if (strcmp(args[0], "ls") == 0) {
        int fd_out = STDOUT_FILENO; // default, to standard output
        int redirect = 0;
        int show_details = 0;
        int mark_files = 0;

        // check flags and redirection
        for (int i = 1; i < argc; i++) {
            if (strcmp(args[i], ">") == 0 && i < argc - 1) {
                fd_out = open(args[i + 1], O_WRONLY | O_CREAT | O_TRUNC, 0644);
                if (fd_out == -1) {
                    perror("failed to open output file");
                    return;
                }
                redirect = 1;
                args[i] = NULL;
                break;
            } else if (strcmp(args[i], "-l") == 0) {
                show_details = 1;
            } else if (strcmp(args[i], "-F") == 0) {
                mark_files = 1;
            }
        }

        DIR *d;
        struct dirent *dir;
        struct stat file_stat;
        char path_buffer[1024];
        d = opendir(".");
        if (d) {
            while ((dir = readdir(d)) != NULL) {
                snprintf(path_buffer, sizeof(path_buffer), "./%s", dir->d_name);
                stat(path_buffer, &file_stat);
                if (show_details) {
                    char time_buffer[256];
                    strftime(time_buffer, sizeof(time_buffer), "%b %d %H:%M", localtime(&file_stat.st_mtime));
                    dprintf(fd_out, "%c%c%c%c%c%c%c%c%c%c %lu %s %s %ld %s %s",
                            (S_ISDIR(file_stat.st_mode)) ? 'd' : '-',
                            (file_stat.st_mode & S_IRUSR) ? 'r' : '-',
                            (file_stat.st_mode & S_IWUSR) ? 'w' : '-',
                            (file_stat.st_mode & S_IXUSR) ? 'x' : '-',
                            (file_stat.st_mode & S_IRGRP) ? 'r' : '-',
                            (file_stat.st_mode & S_IWGRP) ? 'w' : '-',
                            (file_stat.st_mode & S_IXGRP) ? 'x' : '-',
                            (file_stat.st_mode & S_IROTH) ? 'r' : '-',
                            (file_stat.st_mode & S_IWOTH) ? 'w' : '-',
                            (file_stat.st_mode & S_IXOTH) ? 'x' : '-',
                            file_stat.st_nlink, getpwuid(file_stat.st_uid)->pw_name,
                            getgrgid(file_stat.st_gid)->gr_name, file_stat.st_size,
                            time_buffer, dir->d_name);
                } else {
                    dprintf(fd_out, "%s", dir->d_name);
                }

                if (mark_files) {
                    if (S_ISDIR(file_stat.st_mode)) {
                        dprintf(fd_out, "/");
                    } else if (file_stat.st_mode & S_IXUSR) {
                        dprintf(fd_out, "*");
                    }
                }

                dprintf(fd_out, "\n");
            }
            closedir(d);
        } else {
            perror("failed to open directory");
        }

        if (redirect) {
            close(fd_out);
        }
        return;
    }

    // built-in command: cd
    if (strcmp(args[0], "cd") == 0) {
        if (argc < 2) {
            return;
        }
        if (chdir(args[1]) != 0) {
            perror("cd failed");
        }
        return;
    }
    
    // built-in command: chmod
    if (strcmp(args[0], "chmod") == 0) {
        if (argc < 3) {
            fprintf(stderr, "error: wrong arguments\n");
            return;
        }
        mode_t mode = strtol(args[1], NULL, 8);
        if (chmod(args[2], mode) != 0) {
            perror("chmod failed");
        }
        return;
    }

    // built-in command: more
    if (strcmp(args[0], "more") == 0) {
        if (argc < 2) {
            fprintf(stderr, "error: more requires a file name\n");
            return;
        }
        int fd = open(args[1], O_RDONLY);
        if (fd == -1) {
            perror("failed to open file");
            return;
        }

        struct winsize w;
        ioctl(STDOUT_FILENO, TIOCGWINSZ, &w); // get the window size
        int lines = w.ws_row - 1;

        struct termios oldt, newt;
        tcgetattr(STDIN_FILENO, &oldt);
        newt = oldt;
        newt.c_lflag &= ~(ICANON | ECHO);
        tcsetattr(STDIN_FILENO, TCSANOW, &newt);

        char buffer[BUFFER_SIZE];
        int bytes_read;
        int line_count = 0;

        while ((bytes_read = read(fd, buffer, BUFFER_SIZE)) > 0) {
            for (int i = 0; i < bytes_read; i++) {
                write(STDOUT_FILENO, &buffer[i], 1);
                if (buffer[i] == '\n') {
                    line_count++;
                    if (line_count == lines) {
                        while (read(STDIN_FILENO, &buffer[i], 1) && buffer[i] != ' ') {
                            if (buffer[i] == 'q') {
                                tcsetattr(STDIN_FILENO, TCSANOW, &oldt);
                                close(fd);
                                return;
                            }
                        }
                        line_count = 0;
                    }
                }
            }
        }
        tcsetattr(STDIN_FILENO, TCSANOW, &oldt);
        close(fd);
        fprintf(stdout, "\n");
        return;
    }

    // built-in command: jobs
    if (strcmp(args[0], "jobs") == 0){
        printJobList();
        return;
    }

    // built-in command: kill
    if (strcmp(args[0], "kill") == 0){
        int target_jid;
        int index = 1;

        if (args[1] == NULL){ // command: kill
            if (job_tail != NULL) {
                target_jid = job_tail->jid;
            } else {
                printf("no jobs to be killed\n");
                return;
            }
        } else { // command has other arguments
            index = 1;
            if (strcmp(args[1], "-9") == 0){
                index = 2;
            }
            target_jid = atoi(args[index]);
            if (target_jid == 0 && args[index][0] != '0') {
                printf("wrong arguments\n");
                return;
            }
        }
        
        // retrieve the pid of the job to kill
        struct Job* target = job_head;
        while (target != NULL && target->jid != target_jid) {
            target = target->next;
        }
        if (target == NULL) {
            printf("job to kill is not found\n");
            return;
        }

        if (index == 2) { // -9
            kill(target->pid, SIGKILL);
        } else {
            kill(target->pid, SIGTERM);
        }
        return;
    }

    // built-in command: fg
    if (strcmp(args[0], "fg") == 0) {
        int target_jid;

        if (args[1] != NULL) {
            if (args[1][0]=='%'){ // fg %#
                target_jid = atoi(args[1]+1);
                if (target_jid == 0 && args[1][1] != '0') {
                    printf("requires job ID\n");
                    return;
                }
            } else { // fg #
                target_jid = atoi(args[1]);
                if (target_jid == 0 && args[1][0] != '0') {
                    printf("invalid job ID provided\n");
                    return;
                }
            }
        } else { // fg, no specific job number is provided 
            if (job_tail != NULL) {
                target_jid = job_tail->jid;
            } else {
                printf("no job to be fg\n");
                return;
            }
        }
        
        // search for target job
        struct Job* target = NULL;
        for (struct Job* job = job_head; job != NULL; job = job->next) {
            if (job->jid == target_jid) {
                target = job;
                break;
            }
        }
        if (!target) {
            printf("job to fg is not found\n");
            return;
        }

        // bring the job to fg
        tcsetattr(STDIN_FILENO, TCSADRAIN, &target->setting);
        tcsetpgrp(STDIN_FILENO, target->pid);
        if (target->status == suspended) {
            kill(target->pid, SIGCONT); // continue suspended jobs
        }
        int status;
        waitpid(target->pid, &status, WUNTRACED);
        tcsetpgrp(STDIN_FILENO, getpgrp());
        tcsetattr(STDIN_FILENO, TCSADRAIN, &target->setting);

        return;
    }

    // built-in command: bg
    if (strcmp(args[0],"bg") == 0) {
        int target_jid;

        if (args[1] != NULL) {
            if (args[1][0]=='%'){ // bg %#
                target_jid = atoi(args[1]+1);
                if (target_jid == 0 && args[1][1] != '0') {
                    printf("requires job ID\n");
                    return;
                }
            } else { // bg #
                target_jid = atoi(args[1]);
                if (target_jid == 0 && args[1][0] != '0') {
                    printf("invalid job ID provided\n");
                    return;
                }
            }
        } else { // bg, no specific job number is provided
            target_jid = -1;
            for (struct Job* job = job_tail; job != NULL; job = job->prev) {
                if (job->status == suspended) {
                    target_jid = job->jid;
                    break;
                }
            }
            if (target_jid == -1) {
                printf("no job to be bg\n");
                return;
            }
        }
        
        // search for target job
        struct Job* target = NULL;
        for (struct Job* job = job_head; job != NULL; job = job->next) {
            if (job->jid == target_jid) {
                target = job;
                break;
            }
        }
        if (!target) {
            printf("job to bg is not found\n");
            return;
        }

        // continue the job
        if (kill(target->pid, SIGCONT) < 0) {
            perror("failed to send SIGCONT");
            return;
        }
        target->isBG = 1; // mark the job as running in the background
        return;
     }

    // check '&'
    size_t len = strlen(args[argc - 2]);
    if (len > 0 && args[argc - 2][len - 1] == '&') {
        runBG = 1;
        args[argc - 2][len - 1] = '\0';
        if(len == 1) {
            args[argc - 2]= '\0';
        }
    }

    // fork to execute command
    pid_t pid = fork();
    if (pid == 0) {  // child process
        child_handle_signals();

        if (runBG) {
            int fd = open("/dev/null", O_WRONLY);
            if (fd != -1) {
                dup2(fd, STDOUT_FILENO);  // redirect stdout to /dev/null
                dup2(fd, STDERR_FILENO);  // redirect stderr to /dev/null
                close(fd);
            } else {
                perror("failed to open /dev/null");
                exit(EXIT_FAILURE);
            }
        }

        execvp(args[0], args);  
        perror("invalid command"); 
        exit(EXIT_FAILURE);
    } else if (pid > 0) {  // parent process
        insertJob(pid, command, runBG);
        if (!runBG) {
            int status;
            waitpid(pid, &status, WUNTRACED);
        }
    } else {
        perror("failed to fork");
        exit(EXIT_FAILURE);
    }
}


int main() {
    shell_ignore_signals();
    char *input = NULL;

    // handle SIGCHLD
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = sigchld_handler;
    sa.sa_flags = SA_SIGINFO | SA_RESTART;

    sigemptyset(&sa.sa_mask);
    sigaddset(&sa.sa_mask, SIGCHLD);

    if (sigaction(SIGCHLD, &sa, NULL) == -1) {
        perror("sigaction failed");
        exit(EXIT_FAILURE);
    }

    while (TRUE) {
        if (input){
            free(input);
            input = NULL;
        }

        input = readline(PROMPT);
        
        if (!input) { // handle null input
            printf("\n");
            break;
        }
        
        updateJobList();
        if (*input){
            execute_command(input);
        }
    }
    
    // cleanup 
    free(input);
    input = NULL;
    freeDeferredList();
    freeJobList();
    return 0;
}