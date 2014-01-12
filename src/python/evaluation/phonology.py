from loadHITs import *

gitDir = '/Users/etosch/dev/SurveyMan/'
source = gitDir+'data/SMLF5.csv' #sys.argv[1] 
hitDir = '/Users/etosch/Desktop/phonology2/' #sys.argv[2] 

survey = get_survey(source)
make_row_lookups(survey)
responses = load_from_dir(hitDir, survey)
responses_by_id = [{ q.quid : (o.oid, a, b) for (q, (o, a, b)) in response['Answers'].items() } for response in responses]
# align the bot responses -- this will be used for both bot analysis and correlation
# get the words used and create a map from words -> responses
questions_by_word = {


#breakoff analysis
bad_pos, bad_q = evaluation.identify_breakoff_questions(survey, responses_by_id, 0.05)
bad_qs = [ q for q in survey.questions if q.quid in [bq['question'] for bq in bad_q]]

#correlation
