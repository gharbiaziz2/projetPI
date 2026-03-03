package tn.esprit.services;

import tn.esprit.entities.Forum;
import tn.esprit.entities.ForumComment;
import tn.esprit.entities.Voyage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Aggregates sentiment analysis from forum posts, comments, and reactions per voyage.
 */
public class VoyageSentimentService {
    private final ForumServices forumService = new ForumServices();
    private final ForumCommentServices commentService = new ForumCommentServices();
    private final ForumReactionService reactionService = new ForumReactionService();
    private final VoyageServices voyageService = new VoyageServices();
    private final SentimentAnalysisService sentimentService = new SentimentAnalysisService();

    /**
     * Returns sentiment stats per voyage (forum messages + comments analyzed, reactions included).
     */
    public List<VoyageSentimentStats> computeAllVoyageStats() {
        List<VoyageSentimentStats> result = new ArrayList<>();
        try {
            List<Voyage> voyages = voyageService.afficher();
            List<Forum> allForums = forumService.afficher();

            for (Voyage v : voyages) {
                VoyageSentimentStats stats = computeForVoyage(v.getIdVoyage(), v.getNomVoyage(), allForums);
                result.add(stats);
            }
        } catch (SQLException e) {
            System.err.println("VoyageSentiment error: " + e.getMessage());
        }
        return result;
    }

    private VoyageSentimentStats computeForVoyage(int idVoyage, String voyageName, List<Forum> allForums) throws SQLException {
        VoyageSentimentStats s = new VoyageSentimentStats();
        s.voyageId = idVoyage;
        s.voyageName = voyageName != null ? voyageName : "Voyage #" + idVoyage;

        double totalScore = 0;
        int analyzedCount = 0;
        int positiveCount = 0;
        int negativeCount = 0;
        int neutralCount = 0;
        int totalLikes = 0;
        int totalDislikes = 0;

        for (Forum f : allForums) {
            if (f.getIdVoyage() != idVoyage) continue;

            if (f.getContenu() != null && !f.getContenu().isBlank()) {
                var res = sentimentService.analyze(f.getContenu());
                if (res != null) {
                    totalScore += res.score;
                    analyzedCount++;
                    if (res.isPositive()) positiveCount++;
                    else if (res.isNegative()) negativeCount++;
                    else neutralCount++;
                }
            }

            Map<String, Integer> reactions = reactionService.getForumReactionCounts(f.getIdForum());
            totalLikes += reactions.getOrDefault("likes", 0);
            totalDislikes += reactions.getOrDefault("dislikes", 0);

            for (ForumComment c : commentService.getByForumId(f.getIdForum())) {
                if (c.getContenu() != null && !c.getContenu().isBlank()) {
                    var res = sentimentService.analyze(c.getContenu());
                    if (res != null) {
                        totalScore += res.score;
                        analyzedCount++;
                        if (res.isPositive()) positiveCount++;
                        else if (res.isNegative()) negativeCount++;
                        else neutralCount++;
                    }
                }
                Map<String, Integer> cr = reactionService.getCommentReactionCounts(c.getIdComment());
                totalLikes += cr.getOrDefault("likes", 0);
                totalDislikes += cr.getOrDefault("dislikes", 0);
            }
        }

        s.totalAnalyzed = analyzedCount;
        s.positiveCount = positiveCount;
        s.negativeCount = negativeCount;
        s.neutralCount = neutralCount;
        s.totalLikes = totalLikes;
        s.totalDislikes = totalDislikes;
        s.avgSentimentScore = analyzedCount > 0 ? totalScore / analyzedCount : 0;

        int reactionDiff = totalLikes - totalDislikes;
        double reactionWeight = (totalLikes + totalDislikes) > 0
                ? (double) reactionDiff / (totalLikes + totalDislikes)
                : 0;
        s.combinedScore = analyzedCount > 0
                ? (s.avgSentimentScore * 0.7 + reactionWeight * 0.3)
                : reactionWeight;
        return s;
    }

    public static class VoyageSentimentStats {
        public int voyageId;
        public String voyageName;
        public int totalAnalyzed;
        public int positiveCount;
        public int negativeCount;
        public int neutralCount;
        public int totalLikes;
        public int totalDislikes;
        public double avgSentimentScore;
        public double combinedScore;
    }
}
