public class Game {

    private int score;
    private int strangerScore;
    private int orderNum; // Global order number used for every card
    private int revivedCardNum;
    private TwoDAVLTree deck = new TwoDAVLTree();
    private DiscardPileTree discardPile = new DiscardPileTree();

    public String findWinning() {
        if (strangerScore > score) {
            return "The Stranger, Score: " + strangerScore;
        }
        return "The Survivor, Score: " + score;
    }

    public String deckCount() {
        return "Number of cards in the deck: " + deck.size();
    }

    public String discardPileCount() {
        return "Number of cards in the discard pile: " + discardPile.size();
    }

    public String draw_card(String name, int att, int hp) {
        deck.insert(new Card(name, att, hp, orderNum++));
        return "Added " + name + " to the deck";
    }

    public String battle(int att, int hp, int heal_pool) {
        revivedCardNum = 0;

        if (deck.isEmpty()) {
            healCards(heal_pool);
            strangerScore += 2;
            return "No card to play, " + revivedCardNum + " cards revived";
        }

        Card playedCard = null;
        int priority = 1;

        // Check for all priorities in order. Early break if one returns a card
        while (playedCard == null && priority <= 4) {
            playedCard = switch (priority) {
                case 1 -> priorityOne(att, hp);
                case 2 -> priorityTwo(att, hp);
                case 3 -> priorityThree(hp);
                case 4 -> priorityFour();
                default -> null;
            };
            priority++;
        }

        healCards(heal_pool);

        if (playedCard != null) {
//            return String.format("Found with priority %d, Survivor plays %s, the played card %s, %d cards revived",
//                    --priority, playedCard.getName(), priority > 2 ? "is discarded" : "returned to deck", revivedCardNum);

            // This saves 600ms compared to the above code I'm not joking
            // Formatting is very slow for some reason
            return "Found with priority " + --priority +
                    ", Survivor plays " + playedCard.getName() +
                    ", the played card " + (priority > 2 ? "is discarded" : "returned to deck") +
                    ", " + revivedCardNum + " cards revived";
        } else {
            strangerScore += 2;
            return "No card to play, " + revivedCardNum + " cards revived";
        }
    }

    private Card priorityOne(int att, int hp) {
        // Find the smallest outer node with attackValue > hp
        TwoDAVLTree.OuterNode outerNode = deck.findSmallestOuterGreaterThanOrEqual(hp);
        
        // Search through outer nodes (inorder successors) until we find a suitable card
        while (outerNode != null) {
            // Skip this outer node if no card has health > att
            // (major time save!)
            if (outerNode.maxHealth > att) {
                // Search for the smallest card in this outer node's inner tree with health > att
                TwoDAVLTree.InnerNode innerNode = deck.findSmallestInnerGreaterThan(outerNode.innerRoot, att);
                
                if (innerNode != null) {
                    score += 2;
                    strangerScore += 1;

                    return updateCardAfterBattle(innerNode.card, att);
                }
            }
            
            // No suitable card in this outer node, move to next outer node (inorder successor)
            outerNode = deck.getOuterSuccessor(outerNode);
        }
        
        return null;
    }

    private Card updateCardAfterBattle(Card card, int opponentAttack) {
        deck.delete(card);
        card.setHealthCurr(card.getHealthCurr() - opponentAttack);

        // Update orderNum (move to end)
        card.setOrderNum(orderNum++);

        int newAttack = Math.max(1, (int) Math.floor((double) (card.getAttackBase() * card.getHealthCurr()) / card.getHealthBase()));
        card.setAttackCurr(newAttack);
        deck.insert(card);
        
        return card;
    }

    private Card priorityTwo(int att, int hp) {
        // Find the largest outer node with attackValue < hp
        TwoDAVLTree.OuterNode outerNode = deck.findLargestOuterLessThan(hp);
        
        // Search through outer nodes (inorder predecessors) until we find a suitable card
        while (outerNode != null) {
            // Skip this outer node if no card has health > att
            // (major time save!)
            if (outerNode.maxHealth > att) {
                // Search for the smallest card in this outer node's inner tree with health > att
                TwoDAVLTree.InnerNode innerNode = deck.findSmallestInnerGreaterThan(outerNode.innerRoot, att);
                
                if (innerNode != null) {
                    score += 1;
                    strangerScore += 1;
                    
                    return updateCardAfterBattle(innerNode.card, att);
                }
            }
            
            // No suitable card in this outer node, move to previous outer node (inorder predecessor)
            outerNode = deck.getOuterPredecessor(outerNode);
        }
        
        return null;
    }

    private Card priorityThree(int hp) {
        // Find the smallest outer node with attackValue >= hp
        TwoDAVLTree.OuterNode outerNode = deck.findSmallestOuterGreaterThanOrEqual(hp);
        
        // Search through outer nodes (inorder successors) until we find a suitable card
        while (outerNode != null) {
            // Get the minimum card (smallest health, then orderNum) from this outer node
            // No need to check health <= att since priorities 1 & 2 already filtered out all health > att
            TwoDAVLTree.InnerNode innerNode = deck.minInner(outerNode.innerRoot);
            
            if (innerNode != null) {
                // Card will die
                Card card = innerNode.card;
                deck.delete(card);

                card.setHealthMissing(card.getHealthBase());
                card.setOrderNum(orderNum++);
                discardPile.insert(card);
                
                score += 2;
                strangerScore += 2;
                
                return card;
            }
            
            // No suitable card in this outer node, move to next outer node (inorder successor)
            outerNode = deck.getOuterSuccessor(outerNode);
        }
        
        return null;
    }

    private Card priorityFour() {
        // Find the largest outer node (max attack)
        TwoDAVLTree.OuterNode outerNode = deck.maxOuter(deck.root);
        
        if (outerNode != null) {
            // Get the minimum card (smallest health, then orderNum) from this outer node
            TwoDAVLTree.InnerNode innerNode = deck.minInner(outerNode.innerRoot);
            
            if (innerNode != null) {
                // Card will die
                Card card = innerNode.card;
                deck.delete(card);

                card.setHealthMissing(card.getHealthBase());
                card.setOrderNum(orderNum++);
                discardPile.insert(card);
                
                score += 1;
                strangerScore += 2;
                
                return card;
            }
        }
        
        return null;
    }

    public String steal_card(int att, int hp) {
        // Find the smallest outer node with attackValue > att
        TwoDAVLTree.OuterNode outerNode = deck.findSmallestOuterGreaterThanOrEqual(att + 1);
        
        // Search outer nodes (successors) until we find an inner card with health > hp
        while (outerNode != null) {
            // Skip this outer node if no card has health > hp
            // (major time save!)
            if (outerNode.maxHealth > hp) {
                TwoDAVLTree.InnerNode innerNode = deck.findSmallestInnerGreaterThan(outerNode.innerRoot, hp);
                if (innerNode != null) {
                    Card stolen = innerNode.card;
                    deck.delete(stolen);
                    return "The Stranger stole the card: " + stolen.getName();
                }
            }
            outerNode = deck.getOuterSuccessor(outerNode);
        }

        return "No card to steal";
    }

    private void healCards(int heal_pool_points) {
        if (discardPile.isEmpty() || heal_pool_points <= 0) {
            return;
        }

        // Keep finding and fully healing cards with max healthMissing <= heal_pool_points
        Card maxCard = discardPile.findMaxHealthLessThanOrEqual(heal_pool_points);
        int minHealth = discardPile.getMin().getHealthMissing();
        while (maxCard != null) {
            // Fully heal this card
            heal_pool_points -= maxCard.getHealthMissing();
            discardPile.delete(maxCard);
            maxCard.setOrderNum(orderNum++);
            maxCard.setHealthCurr(maxCard.getHealthBase());

            maxCard.setAttackBase(Math.max(1, (int) Math.floor(maxCard.getAttackBase() * 0.9)));
            maxCard.setAttackCurr(maxCard.getAttackBase());

            deck.insert(maxCard);
            revivedCardNum++;

            // Early return
            if (heal_pool_points < minHealth) {
                break;
            }
            
            // Try to find next card that can be fully healed
            if (heal_pool_points > 0) {
                maxCard = discardPile.findMaxHealthLessThanOrEqual(heal_pool_points);
            } else {
                maxCard = null;
            }
        }

        if (discardPile.isEmpty() || heal_pool_points <= 0) {
            return;
        }

        // Partially heal the card with minimum healthMissing
        Card minCard = discardPile.getMin();

        discardPile.delete(minCard);
        minCard.setHealthMissing(minCard.getHealthMissing() - heal_pool_points);
        minCard.setOrderNum(orderNum++);

        minCard.setAttackBase(Math.max(1, (int) Math.floor(minCard.getAttackBase() * 0.95)));

        discardPile.insert(minCard);
    }
}


