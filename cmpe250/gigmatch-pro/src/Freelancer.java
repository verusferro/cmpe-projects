public class Freelancer {
    public final String id;
    private int ratingCount;
    private double ratingAvg;
    private Service service;
    public int[] skills; // T,C,R,E,A
    public int basePrice;
    public int compositeScore;
    public boolean isEmployed;
    public boolean isBurnout;
    public int completedJobs;
    public int cancelledJobs;
    public Customer customer;
    public int cancelledJobsThisMonth;
    public int completedJobsThisMonth;
    public int heapIndex; // Index of location in heap for O(1) removal
    private final MaxHeap[] rankedFreelancers;

    public Freelancer(String id, Service service, int price, int[] skills, MaxHeap[] rankedFreelancers) {
        isEmployed = false;
        isBurnout = false;
        completedJobs = 0;
        cancelledJobs = 0;
        cancelledJobsThisMonth = 0;
        completedJobsThisMonth = 0;
        ratingCount = 1;
        ratingAvg = 5.0;
        heapIndex = -1;
        this.id = id;
        this.service = service;
        this.skills = skills;
        this.basePrice = price;
        this.rankedFreelancers = rankedFreelancers;
        calculateCompositeScore();
    }

    public String getServiceName() {
        return service.name();
    }

    public int getServiceId() {
        return service.ordinal();
    }

    public String getAvgRating() {
        // This is a terrible way to do this whyyy
        return String.format("%.1f", ratingAvg);
    }

    public void addRating(int rating) {
        // This is terrible too please stop
        ratingAvg = ((ratingAvg * ratingCount) + rating) / (ratingCount + 1);
        ratingCount++;
        if (rating >= 4) {
            gainSkills();
        }
        // Update score after rating change
        calculateCompositeScore();
    }

    private void gainSkills() {
        int primarySkill = service.primary.ordinal();
        skills[primarySkill] = Math.min(100, skills[primarySkill] + 2);
        for (Skill skill : service.secondary) {
            int secondarySkill = skill.ordinal();
            skills[secondarySkill] = Math.min(100, skills[secondarySkill] + 1);
        }
    }

    public void degradeSkills() {
        for (int i = 0; i < skills.length; i++) {
            skills[i] = Math.max(0, skills[i] - 3);
        }
        calculateCompositeScore();
    }

    public void updateSkills(int[] newSkills) {
        skills = newSkills;
        calculateCompositeScore();
    }

    public void calculateCompositeScore() {
        double skillScore = calculateSkillScore();
        double ratingScore = calculateRatingScore();
        double reliabilityScore = calculateReliabilityScore();
        double burnoutPenalty = isBurnout ? 0.45 : 0;
        compositeScore = (int) Math.floor(10000 * ((0.55 * skillScore) + (0.25 * ratingScore) +
                                         (0.2 * reliabilityScore) - burnoutPenalty));
        if (!isEmployed) {
            // Update heap location with new score
            rankedFreelancers[service.ordinal()].update(this);
        }
    }

    private double calculateSkillScore() {
        double dotProduct = 0;
        double maximumScore = 0;
        for (int i = 0; i < skills.length; i++) {
            dotProduct += skills[i] * service.getSkill(i);
            maximumScore += service.getSkill(i);
        }
        maximumScore *= 100;
        return dotProduct / maximumScore;
    }

    private double calculateRatingScore() {
        return ratingAvg / 5.0;
    }

    private double calculateReliabilityScore() {
        if (completedJobs + cancelledJobs == 0) {
            return 1.0;
        }
        return 1.0 - (double) cancelledJobs / (completedJobs + cancelledJobs);
        // could just return completedJobs / (completedJobs + cancelledJobs)
        // but precision might return different result so not gonna bother
    }

    public boolean checkPlatformBan() {
        return cancelledJobsThisMonth >= 5;
    }

    public void changeService(Service service, int price) {
        rankedFreelancers[this.service.ordinal()].remove(this);
        this.service = service;
        this.basePrice = price;
        rankedFreelancers[service.ordinal()].insert(this);
        calculateCompositeScore();
    }
}
