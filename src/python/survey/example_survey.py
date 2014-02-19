# -*- coding: cp1252 -*-
#create example survey
#output JSON representation
from survey_representation import *

def main():
    #question 1
    q1 = Question("radio", "What is your gender?",[])
    q1.addOption("Male")
    q1.addOption("Female")
    q1.addOption("Other")
    #print q1
    #question 2
    q2 = Question("dropdown", "What is your year of birth?", [Option(str(x)) for x in range(1950,1996)],[])
    #print q2
    #question 3
    q3 = Question("radio","Which of the following best describes your highest achieved education level?",[])
    q3.addOption("Some High School");
    q3.addOption("High School Graduate");
    q3.addOption("Some College, no Degree");
    q3.addOption("Associates Degree")
    q3.addOption("Bachelors Degree")
    q3.addOption("Graduate Degree, Masters")
    q3.addOption("Graduate Degree, Doctorate")
    #print q3
    #question 4
    q4 = Question("radio", "What is the total income of your household?",[])
    q4.addOption("Less than $10,000")
    q4.addOption("$10,000 - $14,999")
    q4.addOption("$15,000 - $24,999")
    q4.addOption("$25,000 - $39,499")
    q4.addOption("$40,500 - $59,999")
    q4.addOption("$60,000 - $74,999")
    q4.addOption("$75,000 - $99,999")
    q4.addOption("$100,000 - $149,999")
    q4.addOption("More than $150,000")
    #print q4
    #question 5
    q5 = Question("radio", "What is your marital status?",[])
    q5.addOption("Cohabitating")
    q5.addOption("Divorced")
    q5.addOption("Engaged")
    q5.addOption("Married")
    q5.addOption("Separated")
    q5.addOption("Single")
    q5.addOption("Widowed")
    #print q5
    #question 6
    q6 = Question("radio", "Do you have children?",[])
    q6.addOption("No children")
    q6.addOption("Yes, 1 child")
    q6.addOption("Yes, 2 children")
    q6.addOption("Yes, 3 children")
    q6.addOption("Yes, 4 children")
    #print q6
    #question 7
    q7 = Question("radio", "How many members in your household?", [Option(str(x)) for x in range(1,4)])
    #print q7
    #question 8
    q8 = Question("radio", "In which country do you live?",[])
    q8.addOption("United States")
    q8.addOption("India")
    q8.addOption("Other")
    #print q8
    #question 9
    q9 = Question("radio", "Please indicate your race.",[])
    q9.addOption("American Indian or Alaska Native")
    q9.addOption("Asian")
    q9.addOption("Black Latino")
    q9.addOption("Black or African American")
    q9.addOption("Native Hawaiian or Other Pacific Islander")
    q9.addOption("White Latino")
    q9.addOption("White")
    q9.addOption("2 or more races")
    q9.addOption("Unknown")
    #print q9
    #question 10
    q10 = Question("radio", "Why do you complete tasks in Mechanical Turk? Please check any of the following that applies:",[])
    q10.addOption("Fruitful way to spend free time and get some cash (e.g., instead of watching TV).")
    q10.addOption("For “primary” income purposes (e.g., gas, bills, groceries, credit cards).")
    q10.addOption("For “secondary” income purposes, pocket change (for hobbies, gadgets, going out).")
    q10.addOption("To kill time.")
    q10.addOption("I find the tasks to be fun.")
    q10.addOption("I am currently unemployed, or have only a part time job.")
    #question 11
    q11 = Question("dropdown", "Has the recession affected your decision to participate on MTurk?",[])
    q11.addOption("Yes")
    q11.addOption("No")
    #question 12
    q12 = Question("dropdown", "Has the recession affected your level of participation on MTurk?",[])
    q12.addOption("Yes")
    q12.addOption("No")      
    #question 13
    q13 = Question("radio", "For how long have you been working on Amazon Mechanical Turk?",[])
    q13.addOption("< 6 mos.")
    q13.addOption("6mos – 1yr")
    q13.addOption("1-2yrs")
    q13.addOption("2-3yrs")
    q13.addOption("3-5yrs")
    q13.addOption("5-7yrs")
    q13.addOption("7-9yrs")
    q13.addOption("9-15yrs")
    q13.addOption("15+")
    #question 14
    q14 = Question("dropdown","How much do you earn per week on Mechanical Turk?",[])
    q14.addOption("Less than $1 per week")
    q14.addOption("$1-$5 per week.")
    q14.addOption("$5-$10 per week.")
    q14.addOption("$10-$20 per week.")
    q14.addOption("$20-$50 per week.")
    q14.addOption("$50-$100 per week.")
    q14.addOption("$100-$200 per week.")
    q14.addOption("$200-$500 per week.")
    q14.addOption("More than $500 per week.")
    #question 15
    q15 = Question("dropdown","How much time do you spend per week on Mechanical Turk?",[])
    q15.addOption("Less than 1 hour per week.")
    q15.addOption("1-2 hours per week.")
    q15.addOption("2-4 hours per week.")
    q15.addOption("4-8 hours per week.")
    q15.addOption("8-20 hours per week.")
    q15.addOption("20-40 hours per week.")
    q15.addOption("More than 40 hours per week.")
    #question 16
    q16 = Question("dropdown","How many HITs do you complete per week on Mechanical Turk?",[])
    q16.addOption("Less than 1 HIT per week.")
    q16.addOption("1-5 HITs per week.")
    q16.addOption("5-10 HITs per week.")
    q15.addOption("10-20 HITs per week.")
    q16.addOption("20-50 HITs per week.")
    q16.addOption("50-100 HITs per week.")
    q16.addOption("100-200 HITs per week.")
    q16.addOption("200-500 HITs per week.")
    q16.addOption("500-1000 HITs per week.")
    q16.addOption("1000-5000 HITs per week.")
    q16.addOption("More than 5000 HITs per week.")
    

    block1 = Block([q1,q2,q3,q4,q5,q6,q7,q8,q9])
    block3 = Block([q10,q11,q12,q13,q14,q15,q16])
    survey = Survey([block1, block3])
    print survey
    #print survey.jsonize()
    
    
if  __name__ =='__main__':
    main()
