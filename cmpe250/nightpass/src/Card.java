public class Card {

    private final String name;
    private final int attackInit; // The initial values are never used?
    private int attackBase;
    private int attackCurr;
    private final int healthInit;
    private int healthBase;
    private int healthCurr;
    private int healthMissing;
    private int orderNum; // Used to track insertion order to deck/discard pile
    // Gets incremented globally for every card with every operation

    public Card(String name, int att, int hp, int order) {
        this.name = name;
        this.attackInit = att;
        this.attackBase = att;
        this.attackCurr = att;
        this.healthInit = hp;
        this.healthBase = hp;
        this.healthCurr = hp;
        this.orderNum = order;
    }
    
    public String getName() { return name; }
    public int getAttackBase() { return attackBase; }
    public int getAttackCurr() { return attackCurr; }
    public int getHealthBase() { return healthBase; }
    public int getHealthCurr() { return healthCurr; }
    public int getHealthMissing() { return healthMissing; }
    public int getOrderNum() { return orderNum; }
    
    public void setAttackBase(int attackBase) { this.attackBase = attackBase; }
    public void setAttackCurr(int attackCurr) { this.attackCurr = attackCurr; }
    public void setHealthCurr(int healthCurr) { this.healthCurr = healthCurr; }
    public void setHealthMissing(int healthMissing) { this.healthMissing = healthMissing; }
    public void setOrderNum(int orderNum) { this.orderNum = orderNum; }

    // Useful during debugging
    @Override
    public String toString() {
        return name + "(" + attackCurr + "," + healthCurr + "," + orderNum + ")";
    }
}
