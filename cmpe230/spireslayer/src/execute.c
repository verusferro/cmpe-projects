#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <stdlib.h>
#include <assert.h>
#include "tokenizer.c"

// Ingenious and cursed method for generic dynamic arrays
#define da_append(xs, x)\
    do {\
        if ((xs).size >= (xs).capacity) {\
            (xs).capacity = (xs).capacity == 0 ? 8 : (xs).capacity * 2;\
            (xs).items = realloc((xs).items, (xs).capacity * sizeof(*(xs.items)));\
        }\
        (xs).items[(xs).size++] = (x);\
    } while (0)

#define max(a, b) ((a) > (b) ? (a) : (b))
#define min(a, b) ((a) < (b) ? (a) : (b))

typedef struct {
    char* name;
    int baseCount;
    int upgradeCount;
    bool isExhausted;
} Card;

// Data fields necessary for macro method to work
typedef struct {
    Card* items;
    size_t size;
    size_t capacity;
} CardArray;

// Used for Relics and Exhausted Cards
typedef struct {
    char** items;
    size_t size;
    size_t capacity;
} StringArray;

typedef struct {
    char* name;
    int type;
} EnemyWeakness;

// Each enemy has dynamic array for weaknesses
typedef struct {
    char* name;
    EnemyWeakness* items;
    size_t size;
    size_t capacity;
    int defeatedCount;
} CodexEntry;

typedef struct {
    CodexEntry* items;
    size_t size;
    size_t capacity;
} Codex;

typedef struct {
    int gold;
    int currHP;
    int maxHP;
    int floor;
    char currRoom[16];
    CardArray deck;
    StringArray relics;
    char* potionBelt[3]; // stack pointer with easy deletion
    size_t potionBeltIdx;
    Codex codex;
    StringArray exhaustedCards;
} GameState;

GameState state;

void init_game_state(void) {
    state.gold = 0;
    state.currHP = 80;
    state.maxHP = 80;
    state.floor = 0;
    strcpy(state.currRoom, "NONE");
    state.deck = (CardArray){0};
    state.relics = (StringArray){0};
    state.potionBelt[0] = NULL;
    state.potionBelt[1] = NULL;
    state.potionBelt[2] = NULL;
    state.potionBeltIdx = 0;
    state.codex = (Codex){0};
    state.exhaustedCards = (StringArray){0};
}

// Lexicographically sorting comparator for qsort
int compare_cards(const void *a, const void *b) {
    return strcmp(((Card*)a)->name, ((Card*)b)->name);
}

// Same as above
int compare_strings(const void *a, const void *b) {
    return strcmp(*(char* const*)a, *(char* const*)b);
}

int gainGold(int amount) {
    state.gold += amount;
    printf("Gold obtained\n");
    return 0;
}

int gainMaxHP(int amount) {
    state.maxHP += amount;
    printf("Max health increased to %d\n", state.maxHP);
    return 0;
}

int gainItem(char* name, int itemType, int cost) {
    if (itemType == CARD) {
        // Iterate through the card array. This is fast enough for our use case so
        // we didn't bother with implementing a hashmap
        for (size_t i = 0; i < state.deck.size; i++) {
            if (strcmp(state.deck.items[i].name, name) == 0) {
                state.deck.items[i].baseCount++;
                printf("Card added: %s\n", name);
                free(name);
                return 0;
            }
        }
        // Check if the card was marked as exhaust before first addition to deck
        bool exhaust = false;
        for (size_t i = 0; i < state.exhaustedCards.size; i++) {
            if (strcmp(state.exhaustedCards.items[i], name) == 0) {
                exhaust = true;
                break;
            }
        }
        Card c = {.name = name, .baseCount = 1, .upgradeCount = 0, .isExhausted = exhaust};
        da_append(state.deck, c);
        // Sort immediately when new card added to deck so Deck ? query is easy to implement
        qsort(state.deck.items, state.deck.size, sizeof(Card), compare_cards);
        printf("Card added: %s\n", name);
    }
    else if (itemType == RELIC) {
        for (size_t i = 0; i < state.relics.size; i++) {
            if (strcmp(state.relics.items[i], name) == 0) {
                if (cost > 0) {
                    // Since buyItem calls this function, we add back the deducted gold
                    // in case the gain logic fails
                    state.gold += cost;
                }
                printf("Already has relic: %s\n", name);
                free(name);
                return 0;
            }
        }
        da_append(state.relics, name);
        // Immediately sort relics too
        qsort(state.relics.items, state.relics.size, sizeof(char*), compare_strings);
        printf("Relic obtained: %s\n", name);
    }
    else if (itemType == POTION) {
        // Stack based potion belt
        if (state.potionBeltIdx < 3) {
            state.potionBelt[state.potionBeltIdx++] = name;
            printf("Potion obtained: %s\n", name);
            return 0;
        }
        if (cost > 0) {
            state.gold += cost;
        }
        printf("Potion belt is full\n");
        free(name);
    }
    return 0;
}

int buyItem(char* name, int itemType, int cost) {
    if (state.gold < cost) {
        printf("Not enough gold\n");
        free(name);
        return 0;
    }
    state.gold -= cost;
    gainItem(name, itemType, cost);
    return 0;
}

int upgradeCard(char* name) {
    for (size_t i = 0; i < state.deck.size; i++) {
        if (strcmp(state.deck.items[i].name, name) == 0 && state.deck.items[i].baseCount > 0) {
            state.deck.items[i].baseCount--;
            state.deck.items[i].upgradeCount++;
            printf("Card upgraded: %s\n", name);
            return 0;
        }
    }
    printf("Card not found: %s\n", name);
    return 0;
}

int removeCard(char* name, bool isUpgrade) {
    for (size_t i = 0; i < state.deck.size; i++) {
        if (strcmp(state.deck.items[i].name, name) == 0) {
            if (isUpgrade) {
                if (state.deck.items[i].upgradeCount > 0) {
                    state.deck.items[i].upgradeCount--;
                    printf("Upgraded card removed: %s\n", name);
                    return 0;
                }
                else {
                    printf("Upgraded card not found: %s\n", name);
                    return 0;
                }
            }
            else if (!isUpgrade) {
                if (state.deck.items[i].baseCount > 0) {
                    state.deck.items[i].baseCount--;
                    printf("Card removed: %s\n", name);
                    return 0;
                }
                else {
                    printf("Card not found: %s\n", name);
                    return 0;
                }
            }
        }
    }
    if (isUpgrade) {
        printf("Upgraded card not found: %s\n", name);
    }
    else {
    printf("Card not found: %s\n", name);
    }
    return 0;
}

int enterRoom(char* roomType) {
    // Reset the room string
    state.currRoom[0] = '\0';
    // Fixed room name size
    strncpy(state.currRoom, roomType, 16);
    printf("Entered %s room\n", roomType);
    state.floor++;
    return 0;
}

int heal(int amount) {
    state.currHP = min(state.currHP + amount, state.maxHP);
    printf("Ironclad heals to %d\n", state.currHP);
    return 0;
}

int takeDamage(int amount) {
    state.currHP = max(state.currHP - amount, 0);
    printf("Ironclad health drops to %d\n", state.currHP);
    return 0;
}

int discardPotion(char* name) {
    for (size_t i = 0; i < state.potionBeltIdx; i++) {
        if (strcmp(state.potionBelt[i], name) == 0) {
            // Lazy deletion
            free(state.potionBelt[i]);
            state.potionBelt[i] = state.potionBelt[--state.potionBeltIdx];
            printf("Potion discarded: %s\n", name);
            return 0;
        }
    }
    printf("Potion not found: %s\n", name);
    return 0;
}

int sellItem(char* name, int itemType, int number, bool isUpgrade) {
    if (itemType == CARD) {
        for (size_t i = 0; i < state.deck.size; i++) {
            if (strcmp(state.deck.items[i].name, name) == 0) {
                if (isUpgrade) {
                    if (state.deck.items[i].upgradeCount > 0) {
                        state.deck.items[i].upgradeCount--;
                        state.gold += number;
                        printf("Upgraded card sold: %s\n", name);
                        return 0;
                    }
                    else {
                        printf("Upgraded card not found: %s\n", name);
                        return 0;
                    }
                }
                else {
                    if (state.deck.items[i].baseCount > 0) {
                        state.deck.items[i].baseCount--;
                        state.gold += number;
                        printf("Card sold: %s\n", name);
                        return 0;
                    }
                    else {
                        printf("Card not found: %s\n", name);
                        return 0;
                    }
                }
            }
        }
        if (isUpgrade) {
            printf("Upgraded card not found: %s\n", name);
        }
        else {
            printf("Card not found: %s\n", name);
        }
    }
    else if (itemType == POTION) {
        for (size_t i = 0; i < state.potionBeltIdx; i++) {
            if (strcmp(state.potionBelt[i], name) == 0) {
                free(state.potionBelt[i]);
                state.potionBelt[i] = state.potionBelt[--state.potionBeltIdx];
                state.gold += number;
                printf("Potion sold: %s\n", name);
                return 0;
            }
        }
        printf("Potion not found: %s\n", name);
    }
    return 0;
}

int markExhausted(char* name) {
    // Check exhausted cards array first
    for (size_t i = 0; i < state.exhaustedCards.size; i++) {
        if (strcmp(state.exhaustedCards.items[i], name) == 0) {
            printf("Card already exhausts: %s\n", name);
            free(name);
            return 0;
        }
    }
    da_append(state.exhaustedCards, name);
    qsort(state.exhaustedCards.items, state.exhaustedCards.size, sizeof(char*), compare_strings);
    // Then check for cards in deck and mark them
    for (size_t i = 0; i < state.deck.size; i++) {
        if (strcmp(state.deck.items[i].name, name) == 0) {
            if (!state.deck.items[i].isExhausted) {
                state.deck.items[i].isExhausted = true;
                printf("Card marked as exhaust: %s\n", name);
                return 0;
            }
            else {
                printf("Card already exhausts: %s\n", name);
                free(name);
                return 0;
            }
        }
    }
    printf("Card marked as exhaust: %s\n", name);
    return 0;
}

int learnEffective(char* itemName, char* enemyName, int itemType) {
    for (size_t i = 0; i < state.codex.size; i++) {
        if (strcmp(state.codex.items[i].name, enemyName) == 0) {
            for (size_t j = 0; j < state.codex.items[i].size; j++) {
                if (strcmp(state.codex.items[i].items[j].name, itemName) == 0 && state.codex.items[i].items[j].type == itemType) {
                    printf("Effectiveness already noted\n");
                    free(itemName);
                    free(enemyName);
                    return 0;
                }
            }
            EnemyWeakness ew = {.name = itemName, .type = itemType};
            da_append(state.codex.items[i], ew);
            printf("Codex entry updated: %s\n", enemyName);
            free(enemyName);
            return 0;
        }
    }
    CodexEntry ce = {.name = enemyName, .items = NULL, .size = 0, .capacity = 0, .defeatedCount = 0};
    da_append(state.codex, ce);
    EnemyWeakness ew = {.name = itemName, .type = itemType};
    da_append(state.codex.items[state.codex.size - 1], ew);
    printf("Codex entry created: %s\n", enemyName);
    return 0;
}

int fight(char* enemyName, int goldReward) {
    bool won = false;
    for (size_t i = 0; i < state.codex.size; i++) {
        if (strcmp(state.codex.items[i].name, enemyName) == 0) {
            for (size_t j = 0; j < state.codex.items[i].size; j++) {
                EnemyWeakness ew = state.codex.items[i].items[j];
                if (ew.type == CARD) {
                    for (size_t k = 0; k < state.deck.size; k++) {
                        if (strcmp(state.deck.items[k].name, ew.name) == 0 && state.deck.items[k].baseCount + state.deck.items[k].upgradeCount > 0) {
                            if (state.deck.items[k].isExhausted) {
                                if (state.deck.items[k].baseCount > 0) {
                                    state.deck.items[k].baseCount--;
                                }
                                else {
                                    state.deck.items[k].upgradeCount--;
                                }
                            }
                            // Don't immediately return since there can be potions to be discarded
                            won = true;
                            break;
                        }
                    }
                }
                else if (ew.type == RELIC) {
                    for (size_t k = 0; k < state.relics.size; k++) {
                        if (strcmp(state.relics.items[k], ew.name) == 0) {
                            won = true;
                            break;
                        }
                    }
                }
                else if (ew.type == POTION) {
                    for (size_t k = 0; k < state.potionBeltIdx; k++) {
                        if (strcmp(state.potionBelt[k], ew.name) == 0) {
                            free(state.potionBelt[k]);
                            state.potionBelt[k] = state.potionBelt[--state.potionBeltIdx];
                            won = true;
                            break;
                        }
                    }
                }
            }
            if (won) {
                state.codex.items[i].defeatedCount++;
            }
            break;
        }
    }
    if (won) {
        printf("Ironclad defeats %s", enemyName);
        if (goldReward > 0) {
            // Single method for regular and bounty fights
            state.gold += goldReward;
            printf(" and gains %d gold\n", goldReward);
        }
        else {
            printf("\n");
        }
        return 0;
    }
    state.currHP = max(state.currHP - 15, 0);
    printf("Ironclad is outmatched and flees with %d hp remaining\n", state.currHP);
    return 0;
}

int qDeck() {
    if (state.deck.size == 0) {
        printf("None\n");
        return 0;
    }
    bool firstWord = true;
    for (size_t i = 0; i < state.deck.size; i++) {
        // Since cards are already stored sorted, this function is short and sweet
        Card c = state.deck.items[i];
        if (c.baseCount > 0) {
            if (!firstWord) {
                printf(", ");
            }
            printf("%d %s%s", c.baseCount, c.name, c.isExhausted ? "*" : "");
            firstWord = false;
        }
        if (c.upgradeCount > 0) {
            if (!firstWord) {
                printf(", ");
            }
            printf("%d %s+%s", c.upgradeCount, c.name, c.isExhausted ? "*" : "");
            firstWord = false;
        }
    }
    printf("\n");
    return 0;
}

int qGold() {
    printf("%d\n", state.gold);
    return 0;
}

int qFloor() {
    printf("%d\n", state.floor);
    return 0;
}

int qRoom() {
    printf("%s\n", state.currRoom);
    return 0;
}

int qDeckSize() {
    int total = 0;
    for (size_t i = 0; i < state.deck.size; i++) {
        total += state.deck.items[i].baseCount + state.deck.items[i].upgradeCount;
    }
    printf("%d\n", total);
    return 0;
}

int qTotal(char* name) {
    for (size_t i = 0; i < state.deck.size; i++) {
        if (strcmp(state.deck.items[i].name, name) == 0) {
            printf("%d\n", state.deck.items[i].baseCount + state.deck.items[i].upgradeCount);
            return 0;
        }
    }
    printf("0\n");
    return 0;
}

int qTotalUpgraded(char* name) {
    for (size_t i = 0; i < state.deck.size; i++) {
        if (strcmp(state.deck.items[i].name, name) == 0) {
            printf("%d\n", state.deck.items[i].upgradeCount);
            return 0;
        }
    }
    printf("0\n");
    return 0;
}

int qRelics() {
    if (state.relics.size == 0) {
        printf("None\n");
        return 0;
    }
    bool firstWord = true;
    for (size_t i = 0; i < state.relics.size; i++) {
        if (!firstWord) {
            printf(", ");
        }
        printf("%s", state.relics.items[i]);
        firstWord = false;
    }
    printf("\n");
    return 0;
}

int qPotions() {
    // Since potion belt is different than other dynamic arrays, the easiest way for potions is to just create a temp array,
    // sort it, print it, then free it
    if (state.potionBeltIdx == 0) {
        printf("None\n");
        return 0;
    }
    StringArray potions = {0};
    for (size_t i = 0; i < state.potionBeltIdx; i++) {
        da_append(potions, state.potionBelt[i]);
    }
    qsort(potions.items, potions.size, sizeof(char*), compare_strings);
    bool firstWord = true;
    for (size_t i = 0; i < potions.size;) {
        size_t count = 1;
        while (i + count < potions.size && strcmp(potions.items[i], potions.items[i + count]) == 0) {
            count++;
        }
        if (!firstWord) {
            printf(", ");
        }
        printf("%zu %s", count, potions.items[i]);
        firstWord = false;
        i += count;
    }
    printf("\n");
    // Get rid of the temp dynamic array
    free(potions.items);
    return 0;
}

int qExhausts() {
    if (state.exhaustedCards.size == 0) {
        printf("None\n");
        return 0;
    }
    bool firstWord = true;
    for (size_t i = 0; i < state.exhaustedCards.size; i++) {
        if (!firstWord) {
            printf(", ");
        }
        printf("%s", state.exhaustedCards.items[i]);
        firstWord = false;
    }
    printf("\n");
    return 0;
}

int qHealth() {
    printf("%d/%d\n", state.currHP, state.maxHP);
    return 0;
}

int qDefeated(char* name) {
    for (size_t i = 0; i < state.codex.size; i++) {
        if (strcmp(state.codex.items[i].name, name) == 0) {
            printf("%d\n", state.codex.items[i].defeatedCount);
            return 0;
         }
    }
    printf("0\n");
    return 0;
}

int qEffective(char* name) {
    for (size_t i = 0; i < state.codex.size; i++) {
        if (strcmp(state.codex.items[i].name, name) == 0) {
            // First print cards, then relics, then potions. Creating 3 temp arrays is the cleanest solution here
            StringArray cards = {0};
            StringArray relics = {0};
            StringArray potions = {0};

            for (size_t j = 0; j < state.codex.items[i].size; j++) {
                EnemyWeakness ew = state.codex.items[i].items[j];
                // Single loop run to fill all 3 arrays
                if (ew.type == CARD) {
                    da_append(cards, ew.name);
                }
                else if (ew.type == RELIC) {
                    da_append(relics, ew.name);
                }
                else if (ew.type == POTION) {
                    da_append(potions, ew.name);
                }
            }

            qsort(cards.items, cards.size, sizeof(char*), compare_strings);
            qsort(relics.items, relics.size, sizeof(char*), compare_strings);
            qsort(potions.items, potions.size, sizeof(char*), compare_strings);

            bool firstWord = true;
            for (size_t j = 0; j < cards.size; j++) {
                if (!firstWord) {
                    printf(", ");
                }
                printf("card %s", cards.items[j]);
                firstWord = false;
            }
            for (size_t j = 0; j < relics.size; j++) {
                if (!firstWord) {
                    printf(", ");
                }
                printf("relic %s", relics.items[j]);
                firstWord = false;
            }
            for (size_t j = 0; j < potions.size; j++) {
                if (!firstWord) {
                    printf(", ");
                }
                printf("potion %s", potions.items[j]);
                firstWord = false;
            }

            if (firstWord) {
                printf("No codex data for %s\n", name);
            }
            printf("\n");

            free(cards.items);
            free(relics.items);
            free(potions.items);
            return 0;
        }
    }
    printf("No codex data for %s\n", name);
    return 0;
}   
