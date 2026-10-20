import java.util.ArrayList;

public class Matcher {

    HashSet usedIDs = new HashSet();
    HashMap<Customer> customers = new HashMap<>();
    HashMap<Freelancer> freelancers = new HashMap<>();
    // 1 heap per service to store freelancers sorted by score
    MaxHeap[] rankedFreelancers = new MaxHeap[10];
    ArrayList<ServiceChange> serviceChangeQueue = new ArrayList<>();

    public Matcher() {
        for (int i = 0; i < 10; i++) {
            rankedFreelancers[i] = new MaxHeap();
        }
    }

    public String register_customer(String id) {
        if (!usedIDs.add(id)) {
            return "Some error occurred in register_customer.";
        }
        Customer c = new Customer(id);
        customers.put(id, c);
        return "registered customer " + id;
    }

    public String register_freelancer(String id, String service, int price, int T, int C, int R, int E, int A) {
        int[] skills = new int[]{T, C, R, E, A};
        for (int skill : skills) {
            if (skill < 0 || skill > 100) {
                return "Some error occurred in register_freelancer.";
            }
        }
        if (!usedIDs.add(id) || !Service.exists(service) || price < 0) {
            return "Some error occurred in register_freelancer.";
        }
        Service s = Service.valueOf(service);
        Freelancer f = new Freelancer(id, s, price, skills, rankedFreelancers);
        freelancers.put(id, f);
        rankedFreelancers[s.ordinal()].insert(f);
        return "registered freelancer " + id;
    }

    public String update_skill(String id, int T, int C, int R, int E, int A) {
        Freelancer f = freelancers.get(id);
        if (f == null) {
            return "Some error occurred in update_skill.";
        }
        int[] skills = {T, C, R, E, A};
        for (int s : skills) {
            if (s < 0 || s > 100) {
                return "Some error occurred in update_skill.";
            }
        }
        f.updateSkills(skills);
        return "updated skills of " + id + " for " + f.getServiceName();
    }

    public String employ_freelancer(String cust, String free) {
        Customer c = customers.get(cust);
        Freelancer f = freelancers.get(free);
        if (c == null || f == null || c.blacklist.contains(free) || f.isEmployed) {
            return "Some error occurred in employ_freelancer.";
        }
        f.isEmployed = true;
        // rankedFreelancers[f.getServiceId()].remove(f); NOT NEEDED
        // since type 1 doesn't use request_job
        f.customer = c;
        c.totalEmploymentNum++;
        return cust + " employed " + free + " for " + freelancers.get(free).getServiceName();
    }

    public String blacklist(String cust, String free) {
        Customer c = customers.get(cust);
        if (c == null || !freelancers.containsKey(free) || c.blacklist.contains(free)) {
            return "Some error occurred in blacklist.";
        }
        c.blacklist.add(free);
        return cust + " blacklisted " + free;
    }

    public String unblacklist(String cust, String free) {
        Customer c = customers.get(cust);
        if (c == null || !freelancers.containsKey(free) || !c.blacklist.contains(free)) {
            return "Some error occurred in unblacklist.";
        }
        c.blacklist.remove(free);
        return cust + " unblacklisted " + free;
    }

    public String query_freelancer(String id) {
        Freelancer f = freelancers.get(id);
        if (f == null) {
            return "Some error occurred in query_freelancer.";
        }
        return String.format("%s: %s, price: %d, rating: %s, completed: %d, cancelled: %d, skills: " +
                "(%d,%d,%d,%d,%d), available: %s, burnout: %s", id, f.getServiceName(), f.basePrice,
                f.getAvgRating(), f.completedJobs, f.cancelledJobs, f.skills[0], f.skills[1], f.skills[2],
                f.skills[3], f.skills[4], f.isEmployed?"no":"yes", f.isBurnout?"yes":"no");
    }

    public String query_customer(String id) {
        Customer c = customers.get(id);
        if (c == null) {
            return "Some error occurred in query_customer.";
        }
        return String.format("%s: total spent: $%d, loyalty tier: %s, blacklisted freelancer count: %d, " +
                "total employment count: %d", id, c.totalSpentNum, c.loyaltyTier, c.blacklist.size(), c.totalEmploymentNum);
    }

    public String complete_and_rate(String free, int rating) {
        Freelancer f = freelancers.get(free);
        if (f == null || rating < 0 || rating > 5 || !f.isEmployed) {
            return "Some error occurred in complete_and_rate.";
        }
        f.isEmployed = false;
        f.customer.chargeJob(f.basePrice);
        f.completedJobs++;
        f.completedJobsThisMonth++;
        f.addRating(rating);
        rankedFreelancers[f.getServiceId()].insert(f);
        String output = free + " completed job for " + f.customer.id + " with rating " + rating;
        f.customer = null;
        return output;
    }

    public String cancel_by_freelancer(String free) {
        Freelancer f = freelancers.get(free);
        if (f == null || !f.isEmployed) {
            return "Some error occurred in cancel_by_freelancer.";
        }
        f.isEmployed = false;
        f.cancelledJobs++;
        f.cancelledJobsThisMonth++;
        f.addRating(0);
        f.degradeSkills();
        String output = "cancelled by freelancer: " + free + " cancelled " + f.customer.id;
        f.customer = null;
        if (f.checkPlatformBan()) {
            freelancers.remove(free);
            removeQueuedServiceChanges(free);
            output += "\nplatform banned freelancer: " + free;
        } else {
            rankedFreelancers[f.getServiceId()].insert(f);
        }
        return output;
    }

    public String cancel_by_customer(String cust, String free) {
        Customer c = customers.get(cust);
        Freelancer f = freelancers.get(free);
        if (c == null || f == null || !f.isEmployed || f.customer != c) {
            return "Some error occurred in cancel_by_customer.";
        }
        f.isEmployed = false;
        f.customer = null;
        c.cancelledEmploymentNum++;
        rankedFreelancers[f.getServiceId()].insert(f);
        return "cancelled by customer: " + cust + " cancelled " + free;
    }

    public String request_job(String cust, String service, int topK) {
        Customer c = customers.get(cust);
        if (c == null || !Service.exists(service)) {
            return "Some error occurred in request_job.";
        }
        String output;
        // To store removed freelancers during deleteMax to be reinserted later
        ArrayList<Freelancer> removed = new ArrayList<>();
        Service s = Service.valueOf(service);
        int serviceIndex = s.ordinal();
        int foundNum = 0;
        Freelancer bestFreelancer = null;
        MaxHeap searchHeap = rankedFreelancers[serviceIndex];
        if (searchHeap.isEmpty()) {
            return "no freelancers available";
        }
        StringBuilder outputBuilder = new StringBuilder();
        while (foundNum < topK && !searchHeap.isEmpty()) {
            Freelancer f = searchHeap.deleteMax();
            removed.add(f);
            if (c.blacklist.contains(f.id)) {
                continue;
            }
            // Store the top pick
            if (bestFreelancer == null) {
                bestFreelancer = f;
            }
            foundNum++;
            outputBuilder.append(f.id).append(" - composite: ").append(f.compositeScore)
                    .append(", price: ").append(f.basePrice).append(", rating: ")
                    .append(f.getAvgRating()).append("\n");
        }
        output = outputBuilder.toString();
        if (bestFreelancer == null) {
            // Reinsert all removed freelancers
            for (Freelancer f : removed) {
                searchHeap.insert(f);
            }
            return "no freelancers available";
        }
        bestFreelancer.isEmployed = true;
        bestFreelancer.customer = c;
        c.totalEmploymentNum++;
        // Reinsert all removed freelancers except the employed one
        for (Freelancer f : removed) {
            if (f != bestFreelancer) {
                searchHeap.insert(f);
            }
        }
        output = String.format("available freelancers for %s (top %d):\n%s", service, foundNum, output);
        output += "auto-employed best freelancer: " + bestFreelancer.id + " for customer " + cust;
        return output;
    }

    public String change_service(String free, String newService, int newPrice) {
        Freelancer f = freelancers.get(free);
        if (f == null || !Service.exists(newService) || newPrice < 0) {
            return "Some error occurred in change_service.";
        }
        Service service = Service.valueOf(newService);
        serviceChangeQueue.add(new ServiceChange(f, service, newPrice));
        return String.format("service change for %s queued from %s to %s", f.id, f.getServiceName(), newService);
    }

    public String simulate_month() {
        // Update loyalty tiers
        for (Customer c : customers.values()) {
            c.recalculateTier();
        }
        // Burnout and recovery
        for (Freelancer f : freelancers.values()) {
            if (!f.isBurnout && f.completedJobsThisMonth >= 5) {
                f.isBurnout = true;
                f.calculateCompositeScore();
            } else if (f.isBurnout && f.completedJobsThisMonth <= 2) {
                f.isBurnout = false;
                f.calculateCompositeScore();
            }
            f.cancelledJobsThisMonth = 0;
            f.completedJobsThisMonth = 0;
        }
        // Service changes
        for (ServiceChange sc : serviceChangeQueue) {
            Freelancer f = sc.freelancer();
            if (!freelancers.containsKey(f.id)) {
                continue; // skip requests for banned/removed freelancers
            }
            f.changeService(sc.service(), sc.price());
        }
        serviceChangeQueue.clear();
        return "month complete";
    }

    private void removeQueuedServiceChanges(String freelancerId) {
        for (int i = serviceChangeQueue.size() - 1; i >= 0; i--) {
            if (serviceChangeQueue.get(i).freelancer().id.equals(freelancerId)) {
                serviceChangeQueue.remove(i);
            }
        }
    }
}
