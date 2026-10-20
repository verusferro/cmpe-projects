#include <stdio.h>
#include <string.h>
#include "parser.c"

#define MAX_LINE_LENGTH 1024

static void chomp_newline(char *line) {
    size_t len = strlen(line);

    if (len > 0 && line[len - 1] == '\n') {
        line[len - 1] = '\0';
    }
}

// Tokenize -> Parse -> Execute.
int execute_line(char *line) {
    Token token_buff[T_BUFF_SIZE];
	
    tokenize(line, token_buff);

    // We do this so that we don't print at the end an extra line.
    if (token_buff[0].type == EXIT && token_buff[1].type == END) {
        clean(token_buff);
        return 0;
    }

    printf("» ");
    fflush(stdout);

    Tokens token_stack = (Tokens) {.current = 0, .tokens = token_buff };

    int response = parse(&token_stack);
    if(response == 1) {
        printf("INVALID\n");
    }
    
    clean(token_buff);
    return response != -1;
}

int main(void) {
    setvbuf(stdout, NULL, _IOLBF, 0); //fix line eat

    char line[MAX_LINE_LENGTH + 2];

    init_game_state();

    while (1) {
        // printf("» ");
        // printf(">> ");
        // fflush(stdout);
        if (fgets(line, sizeof(line), stdin) == NULL) {
            break;
        }

        chomp_newline(line);

        if (!execute_line(line)) {
            break;
        }
    }
    return 0;
 }
