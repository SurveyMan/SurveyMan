from survey_representation import *

def createSurvey():
    q1 = Question("radio", "Question 1", [Option(str(x)) for x in range(1,4)])
    q2 = Question("radio","Question 2",[Option(str(x)) for x in range(1,4)])
    q3 = Question("radio","Question 3", [Option(str(x)) for x in range(1,4)])

    b1 = Block([q1,q2])
    b2 = Block([q3])
    b4 = Block([Question("radio","Question 4", [])])

    b3 = Block([b1,b2])

    branch = Constraint(q3)
    branch.addBranchByIndex(0,b4)

    survey = Survey([b3,b4],[branch])
    return survey

def main():
    survey = createSurvey()
    print survey
    print survey.jsonize()

if __name__ == '__main__':
    main()
