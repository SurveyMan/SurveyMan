/**
 * Created by IntelliJ IDEA.
 * User: jnewman
 * Date: 6/14/13
 * Time: 11:39 AM
 * To change this template use File | Settings | File Templates.
 */



import com.amazonaws.mturk.addon.HITQuestion;
import com.amazonaws.mturk.requester.QualificationRequirement;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import com.amazonaws.mturk.requester.HIT;

public class SurveyPoster {
    private RequesterService service;
    private String mturkPropertiesPath = "./java/mturk.properties";

    //Defining the attributes of the HIT. These things should be provided by the user somehow...
    private String title = "Take our experimental survey.";
    private String description = "How likely is this as a word of English?";
    private int numAssignments = 1;
    private double reward = 0.05;
    private String keywords = "survey";
    private long assignmentDurationInSeconds = 60 * 60; // 1 hour
    private long autoApprovalDelayInSeconds = 60 * 60 * 24 * 15; // 15 days
    private long lifetimeInSeconds = 60 * 60 * 24 * 3; // 3 days
    private String requesterAnnotation = "";
    QualificationRequirement[] qualReqs = null;
    
    public SurveyPoster() {
        service = new RequesterService(new PropertiesClientConfig(mturkPropertiesPath));
    }

    public boolean hasEnoughFund() {
        double balance = service.getAccountBalance();
        System.out.println("Got account balance: " + RequesterService.formatCurrency(balance));
        return balance > 0;
    }
    
    public void postSurvey(int numSurveys, String surveyDir) {
        String questionFile = "";
        
        for (int i = 0; i < numSurveys; i++) {
            if (i < 10) {
                questionFile = surveyDir + "survey0"+(i+1)+".question";
            }
            else {
                questionFile = surveyDir + "survey"+(i+1)+".question";
            }
            try {
                HITQuestion question = new HITQuestion(questionFile);
                HIT hit = service.createHIT(null, // HITTypeId
                        title,
                        description, keywords,
                        question.getQuestion(),
                        reward, assignmentDurationInSeconds,
                        autoApprovalDelayInSeconds, lifetimeInSeconds,
                        numAssignments, requesterAnnotation,
                        qualReqs,
                        null // responseGroup
                );
                System.out.println("Created HIT: " + hit.getHITId());

                System.out.println("You may see your HIT with HITTypeId '"
                        + hit.getHITTypeId() + "' here: ");

                System.out.println(service.getWebsiteURL()
                        + "/mturk/preview?groupId=" + hit.getHITTypeId());
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.out.println("Error creating HIT.");
                System.exit(0);
            }
        }
    }

}
