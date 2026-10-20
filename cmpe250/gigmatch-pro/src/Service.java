public enum Service {
    // Predetermined primary and secondary skills
    paint(70,60,50,85,90,  Skill.A,  new Skill[]{Skill.T, Skill.E}),
    web_dev(95,75,85,80,90, Skill.T, new Skill[]{Skill.R, Skill.A}),
    graphic_design(75,85,95,70,85, Skill.R, new Skill[]{Skill.C, Skill.A}),
    data_entry(50,50,30,95,95, Skill.E, new Skill[]{Skill.T, Skill.A}),
    tutoring(80,95,70,90,75, Skill.C, new Skill[]{Skill.T, Skill.E}),
    cleaning(40,60,40,90,85, Skill.E, new Skill[]{Skill.C, Skill.A}),
    writing(70,85,90,80,95, Skill.A, new Skill[]{Skill.C, Skill.R}),
    photography(85,80,90,75,90, Skill.R, new Skill[]{Skill.T, Skill.A}),
    plumbing(85,65,60,90,85, Skill.E, new Skill[]{Skill.T, Skill.A}),
    electrical(90,65,70,95,95, Skill.E, new Skill[]{Skill.T, Skill.A});

    private final int[] skills;      // stores all 5 skill values
    public final Skill primary;
    public final Skill[] secondary;

    Service(int t, int c, int r, int e, int a, Skill primary, Skill[] secondary) {
        this.skills = new int[]{t, c, r, e, a};
        this.primary = primary;
        this.secondary = secondary;
    }

    public int get(Skill skill) {
        return skills[skill.ordinal()];
    }

    public int getSkill(int index) {
        return skills[index];
    }

    public static boolean exists(String serviceName) {
        if (serviceName == null) {
            return false;
        }
        for (Service service : Service.values()) {
            if (service.name().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }
}
