import csv

reader = csv.reader(open("/Users/etosch/dev/SurveyMan-public/data/crowdflower_sentiment.csv", "rU"))
header = True
unique_raters = set()
num_judgements = 0
unique_questions = set()
for row in reader:
    if header:
        header = False
    else:
        unique_raters.add(row[3])
        unique_questions.add(row[0])
        num_judgements += 1
        
print "Num raters: %d, Num Questions: %d, Num Judgements: %d" % (len(unique_raters), len(unique_questions), num_judgements)
