from uuid import uuid1

qtypes = {"freetext" : 0 , "radio" : 1 , "check" : 2 , "dropdown" : 3}

class Question : 
    quid = uuid1()
    qtype = ""
    qtext = ""
    options = []
    ok2shuffle = False
    def __init__(self, qtext, options, qtype, shuffle=False):
        self.qtext = qtext
        self.options = options
        self.ok2shuffle = shuffle
        self.qtype=qtype


q1 = Question("What is your age?"
              , makeRadio("< 18", "18-34", "35-64", "> 65")
              , qtypes["radio"])              

q2 = Question("What is your political affiliation?"
              , makeRadio("Democrat", "Republican", "Indepedent")
              , qtypes["radio"]
              , shuffle=True)

q3 = Questions("Which issues do you care about the most?"
               , makeCheckbox("Gun control", "Reproductive Rights", "The Economy", "Foreign Relations")
               qtypes["check"]
               ,shuffle=True)

q4 = Questions("What is your year of birth?"
               , makeDropdown([x+1910 for x in range(80)])
               , qtypes["dropdown"])


               
               
