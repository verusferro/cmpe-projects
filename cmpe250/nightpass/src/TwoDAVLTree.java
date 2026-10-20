// 2D AVL Tree (tree of trees)
// Outer tree sorts by attack value, inner trees sort by (health, orderNum).
// Each outer node represents a unique attack value and contains an inner tree with all the cards
// with that attack value.
public class TwoDAVLTree {
    
    OuterNode root;
    private int totalCards;
    
    public TwoDAVLTree() {
        this.root = null;
        this.totalCards = 0;
    }
    
    public int size() {
        return totalCards;
    }
    
    public boolean isEmpty() {
        return totalCards == 0;
    }
    
    public static class OuterNode {
        int attackValue;
        InnerNode innerRoot;
        int innerSize;
        int maxHealth;  // Track max health for optimization
        OuterNode left, right, parent; // parent data field is for inorder successor finding
        // Removes the need to start search from the root every time (major time save!)
        int height;
        
        OuterNode(int attackValue) {
            this.attackValue = attackValue;
            this.innerRoot = null;
            this.innerSize = 0;
            this.maxHealth = Integer.MIN_VALUE;
            this.height = 1;
        }
    }
    
    public static class InnerNode {
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
        
        int attack = card.getAttackCurr();
        root = insertOuter(root, attack, card);
    }
    
    public void delete(Card card) {
        if (card == null) return;
        
        int attack = card.getAttackCurr();
        root = deleteOuter(root, attack, card);
    }
    
    // Outer tree methods

    public OuterNode maxOuter(OuterNode node) {
        while (node != null && node.right != null) {
            node = node.right;
        }
        return node;
    }
    
    private OuterNode insertOuter(OuterNode node, int attack, Card card) {
        if (node == null) {
            OuterNode newNode = new OuterNode(attack);
            newNode.innerRoot = insertInner(null, card, newNode);
            newNode.innerSize = 1;
            newNode.maxHealth = card.getHealthCurr();
            totalCards++;
            return newNode;
        }
        
        if (attack < node.attackValue) {
            node.left = insertOuter(node.left, attack, card);
            node.left.parent = node;
        } else if (attack > node.attackValue) {
            node.right = insertOuter(node.right, attack, card);
            node.right.parent = node;
        } else {
            // Same attack value - insert into inner tree
            int oldSize = node.innerSize;
            node.innerRoot = insertInner(node.innerRoot, card, node);
            // Only increment if inner insert actually added a node
            // (Had some bugs relating to this)
            if (node.innerSize > oldSize) {
                totalCards++;
                // Update max health
                int cardHealth = card.getHealthCurr();
                if (cardHealth > node.maxHealth) node.maxHealth = cardHealth;
            }
            return node;
        }
        
        updateOuterHeight(node);
        return balanceOuter(node);
    }
    
    private OuterNode deleteOuter(OuterNode node, int attack, Card card) {
        if (node == null) return null;
        
        if (attack < node.attackValue) {
            node.left = deleteOuter(node.left, attack, card);
            if (node.left != null) node.left.parent = node;
        } else if (attack > node.attackValue) {
            node.right = deleteOuter(node.right, attack, card);
            if (node.right != null) node.right.parent = node;
        } else {
            int oldSize = node.innerSize;
            node.innerRoot = deleteInner(node.innerRoot, card, node);
            // Only decrement totalCards if inner delete actually removed a node
            // (Had some bugs relating to this)
            if (node.innerSize < oldSize) {
                totalCards--;
                updateMaxHealth(node);
            }
            
            if (node.innerSize == 0) {
                if (node.left == null) return node.right;
                if (node.right == null) return node.left;
                
                // Two children: replace with inorder successor
                OuterNode successor = minOuter(node.right);
                node.attackValue = successor.attackValue;
                node.innerRoot = successor.innerRoot;
                node.innerSize = successor.innerSize;
                node.maxHealth = successor.maxHealth;
                node.right = deleteOuterNode(node.right, successor.attackValue);
                if (node.right != null) node.right.parent = node;
            }
        }
        
        updateOuterHeight(node);
        return balanceOuter(node);
    }
    
    private OuterNode deleteOuterNode(OuterNode node, int attack) {
        if (node == null) return null;
        
        if (attack < node.attackValue) {
            node.left = deleteOuterNode(node.left, attack);
            if (node.left != null) node.left.parent = node;
        } else if (attack > node.attackValue) {
            node.right = deleteOuterNode(node.right, attack);
            if (node.right != null) node.right.parent = node;
        } else {
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            
            OuterNode successor = minOuter(node.right);
            node.attackValue = successor.attackValue;
            node.innerRoot = successor.innerRoot;
            node.innerSize = successor.innerSize;
            node.right = deleteOuterNode(node.right, successor.attackValue);
            if (node.right != null) node.right.parent = node;
        }
        
        updateOuterHeight(node);
        return balanceOuter(node);
    }
    
    private OuterNode minOuter(OuterNode node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }
    
    // Recalculate max health of an outer node
    private void updateMaxHealth(OuterNode outerNode) {
        if (outerNode.innerRoot == null) {
            outerNode.maxHealth = Integer.MIN_VALUE;
            return;
        }
        
        InnerNode maxNode = maxInner(outerNode.innerRoot);
        outerNode.maxHealth = maxNode.card.getHealthCurr();
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


    // Inner tree operations

    private InnerNode maxInner(InnerNode node) {
        while (node != null && node.right != null) {
            node = node.right;
        }
        return node;
    }

    public InnerNode minInner(InnerNode node) {
        while (node.left != null) {
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

    private int compareInner(Card a, Card b) {
        int healthDiff = Integer.compare(a.getHealthCurr(), b.getHealthCurr());
        if (healthDiff != 0) return healthDiff;
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
    
    // Helper methods
    
    // Find the smallest outer node with attackValue >= limit
    public OuterNode findSmallestOuterGreaterThanOrEqual(int limit) {
        OuterNode candidate = null;
        OuterNode node = root;
        
        while (node != null) {
            if (node.attackValue >= limit) {
                candidate = node;
                node = node.left;
            } else {
                node = node.right;
            }
        }

        return candidate;
    }
    
    // Find the largest outer node with attackValue < limit
    public OuterNode findLargestOuterLessThan(int limit) {
        OuterNode candidate = null;
        OuterNode node = root;
        
        while (node != null) {
            if (node.attackValue < limit) {
                candidate = node;
                node = node.right;
            } else {
                node = node.left;
            }
        }
        
        return candidate;
    }
    
    // Find the smallest inner node with health > limit
    public InnerNode findSmallestInnerGreaterThan(InnerNode root, int limit) {
        InnerNode candidate = null;
        InnerNode node = root;
        
        while (node != null) {
            if (node.card.getHealthCurr() > limit) {
                candidate = node;
                node = node.left;
            } else {
                node = node.right;
            }
        }
        
        return candidate;
    }
    
    // Get the inorder successor of an outer node using parent pointers
    public OuterNode getOuterSuccessor(OuterNode node) {
        if (node == null) return null;
        
        // Node has right subtree, successor is left-most node in right subtree
        if (node.right != null) {
            OuterNode current = node.right;
            while (current.left != null) {
                current = current.left;
            }
            return current;
        }
        
        // No right subtree, go up using parent pointers
        OuterNode parent = node.parent;
        OuterNode current = node;
        
        while (parent != null && current == parent.right) {
            current = parent;
            parent = parent.parent;
        }
        
        return parent;
    }
    
    // Get the inorder predecessor of an outer node using parent pointers
    public OuterNode getOuterPredecessor(OuterNode node) {
        if (node == null) return null;
        
        // Node has left subtree, predecessor is right-most node in left subtree
        if (node.left != null) {
            OuterNode current = node.left;
            while (current.right != null) {
                current = current.right;
            }
            return current;
        }
        
        // No left subtree, go up using parent pointers
        OuterNode parent = node.parent;
        OuterNode current = node;
        
        while (parent != null && current == parent.left) {
            current = parent;
            parent = parent.parent;
        }
        
        return parent;
    }
}
