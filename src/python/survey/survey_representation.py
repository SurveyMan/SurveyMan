#requires Python 2.7.5
#attempt at survey representation
from survey_exceptions import *

class idGenerator:
    """
    Generates ids for survey components; component prefixes are passed as arguments
    (op=option id, s=survey id, q=question id, b=block id, c=constraint id)
    """
    def __init__(self, prefix):
        self.numAssigned=0
        self.prefix=prefix
        
    def generateID(self):
        """Generates a new component id with the appropriate prefix"""
        self.numAssigned+=1
        return self.prefix+str(self.numAssigned)

opGen = idGenerator("op")
surveyGen = idGenerator("s")
qGen = idGenerator("q")
blockGen = idGenerator("b")
constraintGen = idGenerator("c")

class Survey:
    """
    Contains the components of a survey:
    A survey is defined as a list of blocks and a list of branching constraints
    "breakoff" indicates whether the user can quit the survey early
    """

    def __init__(self, blocklist, constraints, breakoff = True):
        """
        Creates a Survey object with a unique id.
        The block list and branch lists are required arguments
        The default value of "breakoff" is true
        """
        #generate ID
        self.surveyID = surveyGen.generateID()
        #survey is a list of blocks, which hold questions and subblocks
        #at least one block with all the questions in it
        self.blockList = blocklist
        #list of branching constraints
        self.constraints = constraints
        self.hasBreakoff = breakoff
        
    def addBlock(self, block):
        """Adds a top level block to the end of the survey's block list"""
        #add block to end of survey (assumed to be a top level block)
        self.blockList.append(block)
        
    def addBlockByIndex(self, block, index):
        """
        Adds a top level block to the desired index in the survey's block list
        Throws index out of bounds exception if index is out of the list's range
        """
        #add block at certain index
        #throws index out of bounds exception (?)
        self.blockList.insert(index, block)

    
    def validate(self):
        """
        Checks that the survey branching is valid before producing the JSON representation
        Confirms that:
            -all blocks follow either the branch-one, branch-all, or branch-none policy
            -all branch questions branch to top-level blocks in the survey's blocklist
            -all branches branch forward
        An exception is thrown if any of these conditions are violated
        """
        #check that all blocks are either branch none, branch one, or branch all
        #change so that it checks subblocks for branching also?
        for b in self.blockList:
            b.validBranchNumber(); #should throw exception if invalid
        
                
        #check that all branches branch to top level blocks in the survey
        for c in self.constraints:
            for bid in c.getBlocks():
                surveyHasBlock = False
                for b in self.blockList:
                    #print("survey block: "+b.blockid + " " +"block branched to: "+bid)
                    if b.blockid == bid or bid == "null":
                        surveyHasBlock = True
                        break
                if surveyHasBlock == False:
                    badBranch = InvalidBranchException("Question "+c.question.qtext+" does not branch to a block in survey")
                    raise badBranch()
            
        #check that all branches branch forward 
        for c in self.constraints:
            branchQuestion = c.question
            #print branchQuestion.block
            blockID = branchQuestion.block.split(".")[0]
            surveyBlockIds = [b.blockid for b in self.blockList]
            for bid in c.getBlocks():
                if(bid !="null" and surveyBlockIds.index(blockID)>=surveyBlockIds.index(bid)):
                    badBranch = InvalidBranchException("Question "+branchQuestion.qtext+" does not branch forward")
                    raise badBranch()
        
    def __str__(self):
        """returns a string representation of the survey"""
        #include some visualization of current branch/block structure?
        output = "Survey ID: "+self.surveyID+"\n"
        for b in self.blockList:
            output = output+str(b)+"\n"
        return output
        
    def jsonize(self):
        """returns the JSON representation of the survey"""
        self.validate()
        if self.hasBreakoff:
            breakoff = "true"
        else:
            breakoff = "false"
        output = "{'breakoff' : '%s', 'survey' : [%s] }" %(breakoff, ",".join([b.jsonize() for b in self.blockList]))
        output = output.replace("\'", "\"")
        return output
    
class Question:
    """
    Contains the components of a survey question:
    Question type is either "radio", "dropdown", "check", or "freetext"
    Question contains text and options, and can be shuffled in a block or fixed
    A question may contain a branchmap
    """

    def __init__(self, qtype, qtext, options, shuffle=True):
        """
        Creates a Question object with a unique id
        Question type, text, and a list of options must be specified
            (option list may be empty)
        Shuffling is allowed by default; user must specify otherwise
        """
        #initialize variables depending on how many arguments provided
        #if you don't want to add options immediately, add empty list as argument
        #call generateID
        self.qid = qGen.generateID()
        self.qtype = qtype
        self.qtext = qtext
        self.options = options
        self.shuffle = shuffle
        self.branching = False
        self.block = "none"

    def addOption(self, oText):
        """
        Creates Option with specified text
        adds it to the end of the question's option list
        """
        o = Option(oText)
        self.options.append(o)

    def addOptionByIndex(self, index, otext):
        """
        Creates Option with specified text
        adds it at the desired index in the question's option list
        throws exception if index is out of list's range
        """
        o = Option(oText)
        self.options.insert(index, o)

    def equals(self, q2):
        """determines if question object is the same question as another object"""
        return self.qid==q2.qid
        
    def __str__(self):
        """returns string representation of the question"""
        text = "Question ID: "+str(self.qid)+" Question type: "+self.qtype+"\n"
        text = text + self.qtext + "\n"
        for o in self.options:
            text = text + "\t" + str(o) + "\n"
        return text

    def jsonize(self):
        """returns JSON representation of the question"""
        if hasattr(self, "branchMap"):
            output = "{'id' : '%s', 'qtext' : '%s', 'options' : [%s], 'branchMap' : %s}"%(self.qid, self.qtext, ",".join([o.jsonize() for o in self.options]), self.branchMap.jsonize())
        else:   
            output = "{'id' : '%s', 'qtext' : '%s', 'options' : [%s]}"%(self.qid, self.qtext, ",".join([o.jsonize() for o in self.options]))
        output = output.replace('\'', '\"');
        return output

class Option:
    """
    Contains the components of an option:
    An option has associated text
    """
    
    def __init__(self, opText):
        """creates an Option object with a unique id and the specified option text"""
        #initialize option text field
        self.opText=opText
        #generate id for option
        self.opid=opGen.generateID()

    def equals(self, o2):
        """determines if option object is the same option as another object"""
        return self.opid==o2.opid

    def jsonize(self):
        """returns the JSON representation of the option"""
        output = "{'id' : '%s', 'otext' : '%s' }" %(self.opid, self.opText)
        output = output.replace('\'', '\"');
        return output
        
    def __str__(self):
        """returns the string representation of the option"""
        return self.opText

class Block:
    """
    Contains the components of a survey Block.
    A block can hold both questions and subblocks
    """

    def subblockIDs(self):
        """
        checks if block contains other blocks, give them appropriate labels.
        Subblocks have ids in the form "parent_id.child_id"
        fixes block labels of the questions contained in the subblocks
        """
        if(len(self.contents) != 0):
            for c in self.contents:
                if(isinstance(c,Block)):
                    c.blockid=self.blockid+(".")+c.blockid
                    c.labelQuestions()
                    
    def labelQuestions(self):
        """labels questions with the id of the block that contains them"""
        if(len(self.contents) != 0):
            for q in self.contents:
                if(isinstance(q,Question)):
                    q.block=self.blockid
        
    def __init__(self, contents, randomize = False):
        """
        creates a Block objects;
        Blocks are a list of contents which could be either subblocks or questions
        Blocks may be randomized; by default, they are not
        """
        self.contents = contents #could contain blocks or questions
        self.blockid = blockGen.generateID()
        self.randomize = randomize
        self.subblockIDs()
        self.labelQuestions()

    def addQuestion(self, question):
        """
        adds question to the end of the Block's list of contents
        labels the question with the containing block's id
        """
        question.block=self.blockid
        self.contents.append(question)
        
    def addSubblock(self, subblock):
        """
        adds a subblock to the end of the Block's list of contents
        labels the subblock with the containing block's id
        """
        subblock.parent=self.blockid
        subblock.blockid = self.blockid+"."+subblock.blockid
        self.contents.append(subblock)

    def getSubblocks(self):
        """returns a list of all the subblocks in the block"""
        subblocks = []
        for c in self.contents:
            if(isinstance(c,Block)):
                subblocks.append(c)
        return subblocks

    def getQuestions(self):
        """returns a list of all the questions in the block"""
        questions = []
        for c in self.contents:
            if(isinstance(c,Question)):
                questions.append(c)
        return questions

    #rethinking how this is done, may move check to Survey object instead
    def validBranchNumber(self):
        """
        checks if there are a valid number of branch questions in the block.
        The three possible policies are branch-one, branch-all, or branch-none
        """
        branching = []
        numQuestions = len(self.getQuestions())
        for q in self.getQuestions():
            if q.branching == True:
                branching.append(q);
                
        if len(branching) == 1:
            #if block contains a branch question, check that none of the subblocks are branch-one
            for b in self.getSubblocks():
                if b.validBranchNumber() == "branch-one":
                    badBranch = InvalidBranchException("Branch-one block cannot contain a branch-one subblock")
                    raise badBranch()
            return "branch-one"
        elif len(branching)==numQuestions and len(branching)!=0:
            #for branch all: check that all questions branch to the same block(s)
            if len(branching)!=0 and hasattr(branching[0], "branchMap"):
                blocksBranchedTo = branching[0].branchMap.getBlocks()
            for q in branching:
                if hasattr(q, "branchMap"):
                    if q.branchMap.getBlocks() != blocksBranchedTo:
                        badBranch = InvalidBranchException("Block branches to different destinations")
                        raise badBranch()
            #check that block does not contain subblocks if branch-all
            if len(self.getSubblocks()) != 0:
                badBranch = InvalidBranchException("Branch-all block cannot contain subblocks")
                raise badBranch()
            return "branch-all"
        elif len(branching)!=0: 
            #throw invalid branch exception
            badBranch = InvalidBranchException("Block contains too many branch questions")
            raise badBranch()
            return "bad-branch"
        else:
            subblockTypes = []
            for b in self.getSubblocks():
                subblockTypes.append(b.validBranchNumber())
            #if there is a branch-one subblock, all of its siblings must be either branch-none or branch-all
            if subblockTypes.count("branch-one")>1:
                badBranch = InvalidBranchException("Block has too many branch-one subblocks")
                raise badBranch()
                return "bad-branch"
            else:
                return "branch-none"                                                   


    def equals(self, block2):
        """determines if block object is the same block as another object"""
        return self.blockid == block2.blockid

    def __str__(self):
        """returns the string representation of the block"""
        output = "Block ID: "+self.blockid+"\n"
        for c in self.contents:
            output=output+str(c)+"\n"
        return output
    
    def jsonize(self):
        """returns the JSON representation of the block"""
        qs=[]
        bs=[]
        for q in self.contents:
            if(isinstance(q, Question)): 
                qs.append(q.jsonize())
            else:
                bs.append(q.jsonize())
        if self.randomize:
            r = "true"
        else:
            r = "false"
        output = "{'id' : '%s', 'questions' : [%s], 'randomize' : '%s', 'subblocks' : [%s] }"%(self.blockid, ",".join(qs), r, ",".join(bs))
        output = output.replace('\'', '\"');
        return output

class Constraint:
    """
    The Constraint object defines a mapping of a question's options to blocks
    in the survey. This is also referred to as branching.
    A branch question has an associated Constraint known as its branch map.
    """
    #defines a mapping from a question options to Blocks
    def __init__(self, question):
        """
        Constructs a Constraint object with a unique id.
        The constraint is associated with the given question, which is labeled as a branch question.
        By default, the options branch to "null" blocks
        """
        self.cid = constraintGen.generateID()
        #add check here to make sure survey contains question
        question.branching = True
        question.branchMap = self
        self.question = question
        #holds list of tuples (opid, blockid)
        self.constraintMap = []
        for o in self.question.options:
            self.constraintMap.append((o.opid, "NEXT"))

    def addBranchByIndex(self, opIndex, block):
        """
        adds a branch from the option at the desired index in the question's option list
        to the desired block.
        Throws an exception if the index is out of the option list's range.
        """
        #throws index out of bounds exception
        self.constraintMap[opIndex] =(self.question.options[opIndex].opid, block.blockid)

    def addBranch(self, op, block):
        """
        adds a branch from a specific option object in the question's option list
        to the desired block.
        Throws an exception if the the question does not have the option.
        """
        for i in range(len(self.question.options)):
            if self.question.options[i].equals(op):
                self.constraintMap[i] = (op.opid, block.blockid)
                return
        noOp = NoSuchOptionException("Question "+self.question.quid+" does not contain option "+opID)
        raise noOp()

    def addBranchByOpText(self, opText, block):
        """
        adds a branch from the option with the desired text in the question's option list
        to the desired block.
        Throws an exception if the the question does not have the option.
        """
        for i in range(len(self.question.options)):
            if self.question.options[i].opText==opText:
                self.constraintMap[i] = (self.question.options[i].opid, block.blockid)
                return
        noOp = NoSuchOptionException("Question "+self.question.quid+" does not contain option \""+opText+'\"')
        raise noOp()

    #returns all blocks branched to by this question
    def getBlocks(self):
        """returns a list of the blocks branched to by the Constraint"""
        output = []
        for c in self.constraintMap:
            output.append(c[1])
        return output

    def __str__(self):
        """returns the string representation of the Constraint"""
        output = "Constraint ID: "+self.cid+"\n"+"branches: \n"
        for (opid, blockID) in self.constraintMap:
            output = output+"\t"+str((opid, blockID))+"\n"
        return output

    def jsonize(self):
        """returns the JSON representation of the Constraint"""
        temp = "";
        cmap = []
        for tup in self.constraintMap:
            temp+="'"+tup[0]+"' : "
            if(tup[1] == "null"):
                temp+="null"
            else:
                temp+="'"+tup[1]+"'"
            cmap.append(temp)
            temp=""
        output = "[%s]"%(",".join(cmap))
        output = output.replace('\'', '\"');
        output= output.replace('[','{')
        output = output.replace(']','}')
        return output
        
        
def main():
    pass
    
    
if  __name__ =='__main__':
    main()


    
