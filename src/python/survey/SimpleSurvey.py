from survey_representation import *

def createSurvey():
    q1 = Question("radio", "Question 1", [Option(str(x)) for x in range(1,4)])
    q2 = Question("radio","Question 2",[Option(str(x)) for x in range(1,4)])
    q3 = Question("radio","Question 3", [Option(str(x)) for x in range(1,4)])

    b1 = Block([q1,q2])
    b2 = Block([q3])
    b3 = Block([b1,b2])

    survey = Survey([b3],[])
    return survey

def main():
    print createSurvey()

if __name__ == '__main__':
    main()
