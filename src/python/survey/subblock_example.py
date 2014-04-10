#example survey based on https://github.com/etosch/SurveyMan/blob/master/data/samples/sample3.csv
#outputs JSON representation

from survey_representation import *

def createSurvey():

    b1 = Block([])
    
    q1 = Question("radio","Block 1a", [])
    q1.addOption("a")
    q1.addOption("b")

    b1.addQuestion(q1)

    q2 = Question("radio","Block 1.1a",[])
    q2.addOption("c")
    q2.addOption("d")

    #trying different ways to add options
    q3 = Question("radio","Block 1.1b",[Option("e"),Option("f")])

    b1_1 = Block([q2,q3])
    b1.addSubblock(b1_1)

    q4 = Question("radio","Block 1b",[Option("g")])

    b1.addQuestion(q4)

    q5 = Question("radio","Block 1.2a",[Option("h")])

    b1_2 = Block([q5])

    q6 = Question("radio","Block 1.2.1b",[Option("X"),Option("Y")])

    q7 = Question("radio","Block 1.2.1a",[Option("j")])

    q8 = Question("radio","Block 1.2.2a",[Option("k")])

    q9 = Question("radio","Block 1.2.2b",[Option("l")])

    q10 = Question("radio","Block 1.2b",[Option("m")])

    b1_2_1 = Block([q6,q7])
    b1_2_2 = Block([q8,q9])

    b1_2.addSubblock(b1_2_1)
    b1_2.addSubblock(b1_2_2)

    q11 = Question("radio","Block 1.3a",[Option("n")])

    q12 = Question("radio","Block 1.3b",[Option("o")])

    q13 = Question("radio","Block 1.3c",[Option("p")])

    b1_3 = Block([q11,q12,q13])

    b1.addSubblock(b1_3)

    q14 = Question("radio","Block 2a",[Option("q"), Option("r")])
                   
    q15 = Question("radio","Block 2c",[Option("s"), Option("t")])

    q16 = Question("radio","Block 3a",[Option("u"), Option("v")])

    b2 = Block([q14,q15])

    b3 = Block([q16])

    q17 = Question("radio","Block 4a", [])
    q17.addOption("w")
    q17.addOption("x")
    q17.addOption("y")
    q17.addOption("z")

    b4 = Block([q17])

    q18 = Question("radio","Block 5.1a",[Option("1"),Option("2"),Option("3")])

    q19 = Question("radio","Block 5.1b",[Option("3"),Option("4")])

    b5_1 = Block([q18,q19])

    q20 = Question("radio","Block 5.2a",[Option("5"),Option("6")])

    q21 = Question("radio","Block 5.2b",[Option("7"),Option("8")])

    b5_2 = Block([q20,q21])

    b5 = Block([b5_1,b5_2])

    survey = Survey([b1,b2,b3,b4,b5],[])

    return survey

def main():
    survey = createSurvey()
    survey.jsonize()                                              

if  __name__ =='__main__':
    main()
