public enum Tier {
    BRONZE, SILVER, GOLD, PLATINUM;

    public double getDiscount() {
        return switch (this) {
            case BRONZE -> 1.0;
            case SILVER -> 0.95;
            case GOLD -> 0.90;
            case PLATINUM -> 0.85;
        };
    }
}
