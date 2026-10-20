#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <stdlib.h>
#include <assert.h>
#include "execute.c"

// Type for stack of tokens, current is the current index.
typedef struct {
    int current;
    Token* tokens;
} Tokens;

// Core
int parseCommand(Tokens* ts);
int parseSentence(Tokens* ts);
int parseQuestion(Tokens* ts);

// Sentence sub-Tokenss
int parseGain(Tokens* ts);
int parseBuy(Tokens* ts);
int parseRemove(Tokens* ts);
int parseUpgrade(Tokens* ts);
int parseEnterRoom(Tokens* ts);
int parseLearn(Tokens* ts);
int parseFight(Tokens* ts);
int parseHeal(Tokens* ts);
int parseDamage(Tokens* ts);
int parseDiscard(Tokens* ts);
int parseSell(Tokens* ts);
int parseMark(Tokens* ts);

// Helpers, makes reading code easier.
int parseName(Tokens* ts);


Token* peek(Tokens* ts) {
    return &(ts->tokens[ts->current]);
}

Token* advance(Tokens* ts) {
    return &(ts->tokens[ts->current++]);
}

int check(Tokens* tsp, TokenType type) {
    return peek(tsp)->type == type;
}

int match(Tokens* ts, TokenType type) {
    if (check(ts, type)) {
        advance(ts);
        return 1;
    }
    return 0;
}
// Start of parse, then branches out based on current token.
int parse(Tokens* ts) {
    if (match(ts, EXIT) && match(ts, END)) return -1;

    if (match(ts, IRONCLAD))
        return parseSentence(ts);

    return parseQuestion(ts);
}
// Same simple logic as above
int parseSentence(Tokens* ts) {
    if (match(ts, GAINS)) return parseGain(ts);
    if (match(ts, BUYS)) return parseBuy(ts);
    if (match(ts, REMOVES)) return parseRemove(ts);
    if (match(ts, UPGRADES)) return parseUpgrade(ts);
    if (match(ts, ENTERS)) return parseEnterRoom(ts);
    if (match(ts, LEARNS)) return parseLearn(ts);
    if (match(ts, FIGHTS)) return parseFight(ts);
    if (match(ts, HEALS)) return parseHeal(ts);
    if (match(ts, TAKES)) return parseDamage(ts);
    if (match(ts, DISCARDS)) return parseDiscard(ts);
    if (match(ts, SELLS)) return parseSell(ts);
    if (match(ts, MARKS)) return parseMark(ts);

    return 1;
}
// Always check for END token, since the token buffer is initialized with all END (and so if it's END then no extra tokens are there.)
// We also see here first the pattern of checking then taking a value from the token stack so we pass to execute.c funcs.
// We sometimes also malloc the names since we free the stack every command.

// We do pretty much the same thing for all funcs so yeah.
int parseGain(Tokens* ts) {
    int number;
    TokenType itemType;
    char* name;
    if (check(ts, NUMBER)) {
        number = ts->tokens[ts->current].literal;
        advance(ts);

        if (match(ts, GOLD) && match(ts, END)) return gainGold(number);

        if (match(ts, MAX)) {
            if (match(ts, HP) && match(ts, END)) return gainMaxHP(number);
            return 1;
        }
        return 1;
    }

    if (check(ts, CARD) || check(ts, RELIC) || check(ts, POTION)) {
        itemType = ts->tokens[ts->current].type; 
        advance(ts);
        if (check(ts, NAME)) {
            name = malloc(strlen(ts->tokens[ts->current].lexeme) + 1);
            strcpy(name, ts->tokens[ts->current].lexeme);
            advance(ts);
            if (match(ts, END)) return gainItem(name, itemType, 0);
            free(name);
        }
    }

    return 1;
}

int parseBuy(Tokens* ts) {
    int itemType;
    char* name;
    int number;
    if (check(ts, CARD) || check(ts, RELIC) || check(ts, POTION)) {
        itemType = ts->tokens[ts->current].type; 
        advance(ts);
        
        if (check(ts, NAME)) {
            name = malloc(strlen(ts->tokens[ts->current].lexeme) + 1);
            strcpy(name, ts->tokens[ts->current].lexeme);
            advance(ts);
        }
        else {
            return 1;
        }
        if (!match(ts, FOR)) {
            free(name);
            return 1;
        }
        if (check(ts, NUMBER)) {
            number = ts->tokens[ts->current].literal;
            advance(ts);
        }
        else {
            free(name);
            return 1;
        };
        if (match(ts, GOLD) && match(ts, END)) return buyItem(name, itemType, number);
        free(name);
        return 1;
    }
    return 1;
}

int parseRemove(Tokens* ts) {
    char* name;
    if (match(ts, CARD)) {
        if(check(ts, NAME)) {
            name = ts->tokens[ts->current].lexeme;
            advance(ts);
            if (match(ts, END)) return removeCard(name, false);
        }
        return 1;
    }

    if (match(ts, UPGRADED)) {
        if (!match(ts, CARD)) return 1;
        if(check(ts, NAME)) {
            name = ts->tokens[ts->current].lexeme;
            advance(ts);
            if (match(ts, END)) return removeCard(name, true);
        }
        return 1;
    }
    return 1;
}
int parseUpgrade(Tokens* ts) {
    char* name;

    if (match(ts, CARD)) {
        if (check(ts, NAME)) {
            name = ts->tokens[ts->current].lexeme;
            advance(ts);
            if (match(ts, END)) return upgradeCard(name);
        }
    }

    return 1;
}

int parseEnterRoom(Tokens* ts) {
    char* roomType;

    if (check(ts, ROOM_TYPE)) {
        roomType = ts->tokens[ts->current].lexeme;
        advance(ts);
        if (match(ts, ROOM)) {
            if (match(ts, END)) return enterRoom(roomType);
        }
    }

    return 1;
}

int parseLearn(Tokens* ts) {
    TokenType itemType;
    char* name1;
    char* name2;

    if (check(ts, CARD) || check(ts, RELIC) || check(ts, POTION)) {
        itemType = ts->tokens[ts->current].type;
        advance(ts);

        if (check(ts, NAME)) {
            name1 = malloc(strlen(ts->tokens[ts->current].lexeme) + 1);
            strcpy(name1, ts->tokens[ts->current].lexeme);
            advance(ts);
        }
        else {
            return 1;
        }

        if(match(ts, IS) && match(ts, EFFECTIVE) && match(ts, AGAINST)) {
                if (check(ts, NAME)) {
                    name2 = malloc(strlen(ts->tokens[ts->current].lexeme) + 1);
                    strcpy(name2, ts->tokens[ts->current].lexeme);
                    advance(ts);

                    if (match(ts, END)) return learnEffective(name1, name2, itemType);
                    free(name2);
            }
        }
        free(name1);
    }
    return 1;
}

int parseFight(Tokens* ts) {
    char* name;
    int number;

    if (check(ts, NAME)) {
        name = ts->tokens[ts->current].lexeme;
        advance(ts);
    } else {
        return 1;
    }

    if (match(ts, FOR)) {
        if (check(ts, NUMBER)) {
            number = ts->tokens[ts->current].literal;
            advance(ts);
        } else {
            return 1;
        }

        if (!match(ts, GOLD)) return 1;
        if (match(ts, END)) return fight(name, number);
    }

    if (match(ts, END)) return fight(name, 0);
    return 1;
}

int parseHeal(Tokens* ts) {
    int number;

    if (check(ts, NUMBER)) {
        number = ts->tokens[ts->current].literal;
        advance(ts);

        if (match(ts, HP) && match(ts, END)) return heal(number);
    }

    return 1;
}

int parseDamage(Tokens* ts) {
    int number;

    if (check(ts, NUMBER)) {
        number = ts->tokens[ts->current].literal;
        advance(ts);

        if (match(ts, DAMAGE) && match(ts, END)) return takeDamage(number);
    }

    return 1;
}

int parseDiscard(Tokens* ts) {
    char* name;

    if (match(ts, POTION)) {
        if (check(ts, NAME)) {
            name = ts->tokens[ts->current].lexeme;
            advance(ts);
            if (match(ts, END)) return discardPotion(name);
        }
    }

    return 1;
}

int parseSell(Tokens* ts) {
    TokenType itemType;
    char* name;
    int number;

    if (check(ts, CARD) || check(ts, POTION)) {
        itemType = ts->tokens[ts->current].type;
        advance(ts);

        if (check(ts, NAME)) {
            name = ts->tokens[ts->current].lexeme;
            advance(ts);
        } else {
            return 1;
        }

        if (!match(ts, FOR)) return 1;

        if (check(ts, NUMBER)) {
            number = ts->tokens[ts->current].literal;
            advance(ts);
        } else {
            return 1;
        }

        if (match(ts, GOLD) && match(ts, END)) return sellItem(name, itemType, number, false);
    }

    if (match(ts, UPGRADED)) {
        if (check(ts, CARD)) {
            itemType = ts->tokens[ts->current].type;
            advance(ts);

            if (check(ts, NAME)) {
                name = ts->tokens[ts->current].lexeme;
                advance(ts);
            } else {
                return 1;
            }

            if (!match(ts, FOR)) return 1;

            if (check(ts, NUMBER)) {
                number = ts->tokens[ts->current].literal;
                advance(ts);
            } else {
                return 1;
            }

            if (match(ts, GOLD) && match(ts, END)) return sellItem(name, itemType, number, true);
        } 
    }

    return 1;
}

int parseMark(Tokens* ts) {
    char* name;

    if (match(ts, CARD)) {
        if (check(ts, NAME)) {
            name = malloc(strlen(ts->tokens[ts->current].lexeme) + 1);
            strcpy(name, ts->tokens[ts->current].lexeme);
            advance(ts);

            if (match(ts, AS) && match(ts, EXHAUST) && match(ts, END))
                return markExhausted(name);
            free(name);
        }
    }

    return 1;
}

int parseQuestion(Tokens* ts) {
    char* name;

    if (match(ts, TOTAL)) {
        if (match(ts, GOLD)) {
            if (match(ts, QUESTION_MARK) && match(ts, END)) return qGold();
            return 1;
        }

        if (match(ts, UPGRADED)) {
            if (match(ts, CARD)) {
                if (check(ts, NAME)) {
                    name = ts->tokens[ts->current].lexeme;
                    advance(ts);

                    if (match(ts, QUESTION_MARK) && match(ts, END)) return qTotalUpgraded(name);
                }
            }
            return 1;
        }

        if (match(ts, CARD)) {
            if (check(ts, NAME)) {
                name = ts->tokens[ts->current].lexeme;
                advance(ts);

                if (match(ts, QUESTION_MARK) && match(ts, END)) return qTotal(name);
                return 1;
            }
        }

        return 1;
    }

    if (match(ts, FLOOR)) {
        if (match(ts, QUESTION_MARK) && match(ts, END)) return qFloor();
        return 1;
    }

    if (match(ts, WHERE)) {
        if (match(ts, QUESTION_MARK) && match(ts, END)) return qRoom();
        return 1;
    }

    if (match(ts, DECK)) {
        if (match(ts, SIZE)) {
            if (match(ts, QUESTION_MARK) && match(ts, END)) return qDeckSize();
            return 1;
        }

        if (match(ts, QUESTION_MARK) && match(ts, END)) return qDeck();
        return 1;
    }

    if (match(ts, RELICS)) {
        if (match(ts, QUESTION_MARK) && match(ts, END)) return qRelics();
        return 1;
    }

    if (match(ts, POTIONS)) {
        if (match(ts, QUESTION_MARK) && match(ts, END)) return qPotions();
        return 1;
    }

    if(match(ts, WHAT) && match(ts, IS) && match(ts, EFFECTIVE) && match(ts, AGAINST)) {
        if (check(ts, NAME)) {
            name = ts->tokens[ts->current].lexeme;
            advance(ts);

            if (match(ts, QUESTION_MARK) && match(ts, END)) return qEffective(name);
            return 1;
        }
    }

    if (match(ts, DEFEATED)) {
        if (check(ts, NAME)) {
            name = ts->tokens[ts->current].lexeme;
            advance(ts);

            if (match(ts, QUESTION_MARK) && match(ts, END)) return qDefeated(name);
            return 1;
        }
    }

    if (match(ts, HEALTH)) {
        if (match(ts, QUESTION_MARK) && match(ts, END)) return qHealth();
        return 1;
    }

   if (match(ts, EXHAUSTS)) {
        if (match(ts, QUESTION_MARK) && match(ts, END)) return qExhausts();
        return 1;
    } 
    return 1;
}
