public class Customer {
    public final String id;
    public final HashSet blacklist;
    public int totalSpentNum;
    public int totalEmploymentNum;
    public int cancelledEmploymentNum;
    public Tier loyaltyTier;

    public Customer(String id) {
        blacklist = new HashSet();
        totalSpentNum = 0;
        totalEmploymentNum = 0;
        loyaltyTier = Tier.BRONZE;
        this.id = id;
    }

    public void chargeJob(int price) {
        totalSpentNum += (int) Math.floor(price * loyaltyTier.getDiscount());
    }

    public void recalculateTier() {
        int amount = totalSpentNum - (250 * cancelledEmploymentNum);
        if (amount >= 5000) {
            loyaltyTier = Tier.PLATINUM;
        } else if (amount >= 2000) {
            loyaltyTier = Tier.GOLD;
        } else if (amount >= 500) {
            loyaltyTier = Tier.SILVER;
        } else {
            loyaltyTier = Tier.BRONZE;
        }
    }
}
