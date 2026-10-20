#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <stdlib.h>
#include <assert.h>
#include <stdint.h>

// NOTE(dan): I'm going to return an array of size 1024 so that we don't overflow (the command size limit is also 1024).
//			  I also think that tokenizing per command is better than the entire file because we then have an upper bound.
#define T_BUFF_SIZE 1024

// We'll use this for checking.
char* reserved[] = {"Ironclad", "gains", "gold", "max", "hp", "card", "relic", "potion", "buys", "for", "removes",
					 "upgraded", "upgrades", "enters", "room", "learns", "is", "effective", "against", "fights",
					 "heals", "takes", "damage", "discards", "sells", "marks", "as", "exhaust", "Total", "Floor",
					 "Where", "Deck", "size", "Relics", "Potions", "What", "Defeated", "Health", "Exhausts", "Exit", 0};

char* rooms[] = {"Monster", "Elite", "Rest", "Shop", "Treasure", "Event", "Boss", 0};

typedef enum {
		
	// KEYWORDS
	IRONCLAD, GAINS, GOLD, MAX, HP, CARD, RELIC, POTION, BUYS, FOR, REMOVES, UPGRADED, UPGRADES,
	ENTERS, ROOM, LEARNS,IS, EFFECTIVE, AGAINST, FIGHTS, HEALS, TAKES, DAMAGE, DISCARDS, SELLS, 
	MARKS, AS, EXHAUST, TOTAL, FLOOR, WHERE, DECK, SIZE, RELICS, POTIONS, WHAT, DEFEATED, HEALTH, 
	EXHAUSTS, EXIT,

	//TYPES
	NUMBER, NAME, ROOM_TYPE,
	
	//EXTRA
	QUESTION_MARK,

	//DEBUGGING AND CONTROL
	END,
	INVALID,
	TOKEN_TYPE_COUNT

} TokenType;

typedef struct {
	TokenType type;
	int literal;
	char* lexeme;
} Token; 

void clear(Token* token_buff) {
	Token token = {.lexeme = "", .literal = -1, .type = END}; 
	for (int i = 0; i < T_BUFF_SIZE; i++) {
        token_buff[i] = token;
    }
}

void clean(Token* token_buff) {
	for (int i = 0; i < T_BUFF_SIZE; i++) {
		if (token_buff[i].type != END) {
			free(token_buff[i].lexeme);
		}
	}
}
// Start and end index inclusive. 
char* substring(char* orig, int start, int end) {
	assert(end >= start);

	int size = end - start + 1;
	char *sub = malloc(size + 1);

	for(int i = 0; i < size; i++) { sub[i] = orig[start + i]; }

	sub[size] = '\0';

	return sub;
}

bool isLetter(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z');
}

bool isDigit(char c) {
	return c >= '0' && c <= '9';
}

int findKeyword(char* word) {
	int i = 0;
	while(reserved[i]) {
        if(strcmp(reserved[i], word) == 0) {
           return i;
        }
        i++;
    }
	return -1;
}

int findRoom(char* word) {
	int i = 0;
	while(rooms[i]) {
        if(strcmp(rooms[i], word) == 0) {
           return i;
        }
        i++;
    }
	return -1;
}

// Note(dan): I use this to validate names and properly glob them together (can't have keyword inside name)
bool isNextKeyword(char *line, int start_pos) {
	int offset = 0;

	while(isLetter(line[start_pos + offset + 1])) {offset++;}
	char* lexeme = substring(line, start_pos, start_pos + offset);
	// Returns index in the keyword map which is also the type enum.
	int type = findKeyword(lexeme);

	free(lexeme);

	if (type == -1) return false;

	return true;
}

void addToken(Token* token_buff, int* token_index, char* lexeme, int literal, TokenType type) {
	token_buff[*token_index] = (Token) {.lexeme = lexeme, .literal = literal, .type = type};
	(*token_index)++;	
 }

Token* tokenize(char* line, Token* token_buff) {
	clear(token_buff);

	int token_index = 0;
	int current = 0;
	int start_pos = 0;

	// Glob all invalid characters until end or whitespace.
	while (line[current] != '\0') {
		char c = line[current];

		switch (c) {
			case ' ': break;
			// Special case for questions, the INVALID check is the same.
			case '?': 
				if((line[current + 1] == ' ' || line[current + 1] == '\0')) {
					addToken(token_buff, &token_index, substring(line, start_pos, current), -1, QUESTION_MARK); break;
				}
				while(line[current + 1] != ' ' && line[current + 1] != '\0') {
						current++;
				}
				addToken(token_buff, &token_index, substring(line, start_pos, current), -1, INVALID); break;
            default:
				// Number token logic, this also excludes numbers that start with 0 and turns them into INVALID tokens.
                if (isDigit(c)) {
                    while(isDigit(line[current + 1])){current++;}
					if((line[current + 1] == ' ' || line[current + 1] == '\0') && c != '0') {
						char* lexeme = substring(line, start_pos, current);
						addToken(token_buff, &token_index, lexeme, (int)strtoul(lexeme, NULL, 10), NUMBER);
						break;
					}
					// For taking the entirety of the INVALID token.
					while(line[current + 1] != ' ' && line[current + 1] != '\0') {
						current++;
					}

					addToken(token_buff, &token_index, substring(line, start_pos, current), -1, INVALID);
                } 
				// Names and keywords. (And roomtypes)
				else if (isLetter(c)) {
                    while(isLetter(line[current + 1])){current++;}
					char* lexeme = substring(line, start_pos, current);
					// Returns index in the keyword map which is also the type enum.
					int type = findKeyword(lexeme);
					int room = findRoom(lexeme);
					free(lexeme);
					// Checks if keyword and proceses, the invalid logic is the same.
					if (type != -1) {
						if (line[current + 1] == ' ' || line[current + 1] == '\0') {
							
							addToken(token_buff, &token_index, substring(line, start_pos, current), -1, type);
							break;
						}
						while(line[current + 1] != ' ' && line[current + 1] != '\0') {
								current++;
						}
						addToken(token_buff, &token_index, substring(line, start_pos, current) , -1, INVALID);	
						break;
					}
					// Same as the above but for room types.
					if (room != -1) {
						if (line[current + 1] == ' ' || line[current + 1] == '\0') {
							
							addToken(token_buff, &token_index, substring(line, start_pos, current), -1, ROOM_TYPE);
							break;
						}
						while(line[current + 1] != ' ' && line[current + 1] != '\0') {
								current++;
						}
						addToken(token_buff, &token_index, substring(line, start_pos, current) , -1, INVALID);	
						break;
					}
					// Checking for names, can't have keyword after a name so we check if next word is keyword.
				    while (line[current + 1] == ' ' && isLetter(line[current + 2]) && !isNextKeyword(line, current + 2)) {
						current++;
						while(isLetter(line[current + 1])){current++;}
				    }
					if(line[current + 1] == ' ' || line[current + 1] == '\0') {
						addToken(token_buff, &token_index, substring(line, start_pos, current), -1, NAME);
						break;
					}
					while(line[current + 1] != ' ' && line[current + 1] != '\0') {
						current++;
					}
					addToken(token_buff, &token_index, substring(line, start_pos, current), -1, INVALID);	
					
                }
				// If matches no valid chars.
				else {
					addToken(token_buff, &token_index, substring(line, start_pos, current), -1, INVALID);
					break;
				}
			break;
		}
		current++;
		start_pos = current;
	}

	return token_buff;
}
