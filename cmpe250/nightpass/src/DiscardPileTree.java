// Just a modified version of TwoDAVLTree class.
// I found the effort needed to modify the original class to be modular was not worth it.
// Outer tree sorts by healthMissing, inner trees sort by orderNum.
// Each outer node represents a unique healthMissing value and contains an inner tree with all the cards
// with that healthMissing value.
public class DiscardPileTree {
    
    OuterNode root;
    private int totalCards;
    
    public DiscardPileTree() {
        this.root = null;
        this.totalCards = 0;
    }
    
    public int size() {
        return totalCards;
    }
    
    public boolean isEmpty() {
        return totalCards == 0;
    }
    
    static class OuterNode {
        int healthMissing;
        InnerNode innerRoot;
        int innerSize;
        OuterNode left, right, parent; // parent data field is for inorder successor finding
        // Removes the need to start search from the root every time (major time save!)
        int height;
        
        OuterNode(int healthMissing) {
            this.healthMissing = healthMissing;
            this.innerRoot = null;
            this.innerSize = 0;
            this.height = 1;
        }
    }
    
    static class InnerNode {
        Card card;
        InnerNode left, right; // parent isn't needed for inner node
        int height;
        
        InnerNode(Card card) {
            this.card = card;
            this.height = 1;
        }
    }
    
    public void insert(Card card) {
        if (card == null) throw new IllegalArgumentException("Card cannot be null");
        
        int healthMissing = card.getHealthMissing();
        root = insertOuter(root, healthMissing, card);
    }

    public void delete(Card card) {
        if (card == null) return;

        int healthMissing = card.getHealthMissing();
        root = deleteOuter(root, healthMissing, card);
    }

    // Get the minimum/smallest node (left-most node of left-most tree)
    public Card getMin() {
        if (root == null) return null;
        OuterNode minOuter = minOuter(root);
        if (minOuter == null || minOuter.innerRoot == null) return null;
        InnerNode minInner = minInner(minOuter.innerRoot);
        return minInner != null ? minInner.card : null;
    }

    // Find the card with maximum healthMissing <= limit
    // Among cards with that healthMissing, returns the one with minimum orderNum
    public Card findMaxHealthLessThanOrEqual(int limit) {
        OuterNode outerNode = findLargestOuterLessThanOrEqual(root, limit);
        if (outerNode == null || outerNode.innerRoot == null) return null;
        // Get the leftmost (minimum orderNum) card
        InnerNode minInner = minInner(outerNode.innerRoot);
        return minInner != null ? minInner.card : null;
    }

    // Outer tree methods

    // Find the largest outer node with healthMissing <= limit.
    private OuterNode findLargestOuterLessThanOrEqual(OuterNode node, int limit) {
        OuterNode candidate = null;

        while (node != null) {
            if (node.healthMissing <= limit) {
                candidate = node;
                node = node.right;
            } else {
                node = node.left;
            }
        }

        return candidate;
    }

    private OuterNode insertOuter(OuterNode node, int healthMissing, Card card) {
        if (node == null) {
            OuterNode newNode = new OuterNode(healthMissing);
            newNode.innerRoot = insertInner(null, card, newNode);
            newNode.innerSize = 1;
            totalCards++;
            return newNode;
        }

        if (healthMissing < node.healthMissing) {
            node.left = insertOuter(node.left, healthMissing, card);
            node.left.parent = node;
        } else if (healthMissing > node.healthMissing) {
            node.right = insertOuter(node.right, healthMissing, card);
            node.right.parent = node;
        } else {
            // Same healthMissing value - insert into inner tree
            int oldSize = node.innerSize;
            node.innerRoot = insertInner(node.innerRoot, card, node);
            // Only increment if inner insert actually added a node
            // (Had some bugs relating to this)
            if (node.innerSize > oldSize) {
                totalCards++;
            }
            return node;
        }

        updateOuterHeight(node);
        return balanceOuter(node);
    }

    private OuterNode deleteOuter(OuterNode node, int healthMissing, Card card) {
        if (node == null) return null;

        if (healthMissing < node.healthMissing) {
            node.left = deleteOuter(node.left, healthMissing, card);
            if (node.left != null) node.left.parent = node;
        } else if (healthMissing > node.healthMissing) {
            node.right = deleteOuter(node.right, healthMissing, card);
            if (node.right != null) node.right.parent = node;
        } else {
            int oldSize = node.innerSize;
            node.innerRoot = deleteInner(node.innerRoot, card, node);
            // Only decrement totalCards if inner delete actually removed a node
            // (Had some bugs relating to this)
            if (node.innerSize < oldSize) {
                totalCards--;
            }

            if (node.innerSize == 0) {
                if (node.left == null) return node.right;
                if (node.right == null) return node.left;

                OuterNode successor = minOuter(node.right);
                node.healthMissing = successor.healthMissing;
                node.innerRoot = successor.innerRoot;
                node.innerSize = successor.innerSize;
                node.right = deleteOuterNode(node.right, successor.healthMissing);
                if (node.right != null) node.right.parent = node;
            }
        }

        updateOuterHeight(node);
        return balanceOuter(node);
    }

    private OuterNode deleteOuterNode(OuterNode node, int healthMissing) {
        if (node == null) return null;

        if (healthMissing < node.healthMissing) {
            node.left = deleteOuterNode(node.left, healthMissing);
            if (node.left != null) node.left.parent = node;
        } else if (healthMissing > node.healthMissing) {
            node.right = deleteOuterNode(node.right, healthMissing);
            if (node.right != null) node.right.parent = node;
        } else {
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;

            OuterNode successor = minOuter(node.right);
            node.healthMissing = successor.healthMissing;
            node.innerRoot = successor.innerRoot;
            node.innerSize = successor.innerSize;
            node.right = deleteOuterNode(node.right, successor.healthMissing);
            if (node.right != null) node.right.parent = node;
        }

        updateOuterHeight(node);
        return balanceOuter(node);
    }

    private OuterNode minOuter(OuterNode node) {
        while (node != null && node.left != null) {
            node = node.left;
        }
        return node;
    }

    private int outerHeight(OuterNode node) {
        return node == null ? 0 : node.height;
    }
    
    private void updateOuterHeight(OuterNode node) {
        if (node != null) {
            node.height = 1 + Math.max(outerHeight(node.left), outerHeight(node.right));
        }
    }
    
    private int getOuterBalance(OuterNode node) {
        return node == null ? 0 : outerHeight(node.left) - outerHeight(node.right);
    }
    
    private OuterNode balanceOuter(OuterNode node) {
        if (node == null) return null;
        
        int balance = getOuterBalance(node);
        
        // Left heavy
        if (balance > 1) {
            if (getOuterBalance(node.left) < 0) {
                node.left = rotateOuterLeft(node.left);
            }
            return rotateOuterRight(node);
        }
        
        // Right heavy
        if (balance < -1) {
            if (getOuterBalance(node.right) > 0) {
                node.right = rotateOuterRight(node.right);
            }
            return rotateOuterLeft(node);
        }
        
        return node;
    }
    
    private OuterNode rotateOuterRight(OuterNode node) {
        OuterNode leftChild = node.left;
        OuterNode temp = leftChild.right;
        
        leftChild.right = node;
        node.left = temp;
        
        // Update parents
        leftChild.parent = node.parent;
        node.parent = leftChild;
        if (temp != null) temp.parent = node;
        
        updateOuterHeight(node);
        updateOuterHeight(leftChild);
        
        return leftChild;
    }
    
    private OuterNode rotateOuterLeft(OuterNode node) {
        OuterNode rightChild = node.right;
        OuterNode temp = rightChild.left;
        
        rightChild.left = node;
        node.right = temp;
        
        // Update parents
        rightChild.parent = node.parent;
        node.parent = rightChild;
        if (temp != null) temp.parent = node;
        
        updateOuterHeight(node);
        updateOuterHeight(rightChild);
        
        return rightChild;
    }
    
    // Inner tree methods

    private InnerNode minInner(InnerNode node) {
        while (node != null && node.left != null) {
            node = node.left;
        }
        return node;
    }

    private InnerNode insertInner(InnerNode node, Card card, OuterNode outerNode) {
        if (node == null) {
            InnerNode newNode = new InnerNode(card);
            outerNode.innerSize++; // Increment when actually inserting
            return newNode;
        }

        int cmp = compareInner(card, node.card);

        if (cmp < 0) {
            node.left = insertInner(node.left, card, outerNode);
        } else if (cmp > 0) {
            node.right = insertInner(node.right, card, outerNode);
        } else {
            // Duplicate
            return node;
        }

        updateInnerHeight(node);
        return balanceInner(node);
    }

    private InnerNode deleteInner(InnerNode node, Card card, OuterNode outerNode) {
        if (node == null) return null;

        int cmp = compareInner(card, node.card);

        if (cmp < 0) {
            node.left = deleteInner(node.left, card, outerNode);
        } else if (cmp > 0) {
            node.right = deleteInner(node.right, card, outerNode);
        } else {
            outerNode.innerSize--; // Decrement when actually deleting

            if (node.left == null) {
                return node.right;
            }
            if (node.right == null) {
                return node.left;
            }

            InnerNode successor = minInner(node.right);
            node.card = successor.card;
            // Delete successor - increment to compensate since we're not removing a node
            outerNode.innerSize++;
            node.right = deleteInner(node.right, successor.card, outerNode);
        }

        updateInnerHeight(node);
        return balanceInner(node);
    }

    // Compare by orderNum
    private int compareInner(Card a, Card b) {
        return Integer.compare(a.getOrderNum(), b.getOrderNum());
    }
    
    private int innerHeight(InnerNode node) {
        return node == null ? 0 : node.height;
    }
    
    private void updateInnerHeight(InnerNode node) {
        if (node != null) {
            node.height = 1 + Math.max(innerHeight(node.left), innerHeight(node.right));
        }
    }
    
    private int getInnerBalance(InnerNode node) {
        return node == null ? 0 : innerHeight(node.left) - innerHeight(node.right);
    }
    
    private InnerNode balanceInner(InnerNode node) {
        if (node == null) return null;
        
        int balance = getInnerBalance(node);
        
        // Left heavy
        if (balance > 1) {
            if (getInnerBalance(node.left) < 0) {
                node.left = rotateInnerLeft(node.left);
            }
            return rotateInnerRight(node);
        }
        
        // Right heavy
        if (balance < -1) {
            if (getInnerBalance(node.right) > 0) {
                node.right = rotateInnerRight(node.right);
            }
            return rotateInnerLeft(node);
        }
        
        return node;
    }
    
    private InnerNode rotateInnerRight(InnerNode node) {
        InnerNode leftChild = node.left;
        InnerNode temp = leftChild.right;
        
        leftChild.right = node;
        node.left = temp;
        
        updateInnerHeight(node);
        updateInnerHeight(leftChild);
        
        return leftChild;
    }
    
    private InnerNode rotateInnerLeft(InnerNode node) {
        InnerNode rightChild = node.right;
        InnerNode temp = rightChild.left;
        
        rightChild.left = node;
        node.right = temp;
        
        updateInnerHeight(node);
        updateInnerHeight(rightChild);
        
        return rightChild;
    }
}
