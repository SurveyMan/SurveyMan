import unittest
from SurveyMan.survey.survey_representation import *
from SurveyMan.survey.survey_exceptions import * 
import SurveyMan.examples.subblock_example as sbEx
import SurveyMan.examples.example_survey as ex
import SurveyMan.examples.SimpleSurvey as simpEx

class BlockTests(unittest.TestCase):

    def setUp(self):
        self.blockSurvey = sbEx.createSurvey()
        self.ipierotisSurvey = ex.createSurvey()
        self.simpleSurvey = simpEx.createSurvey()

    def tearDown(self):
        del self.blockSurvey
        del self.ipierotisSurvey
        del self.simpleSurvey

    def countBlocks(self,blockList):
        if(len(blockList)==0):
            return 0
        else:
            sumSubblocks = 0
            for b in blockList:
                sumSubblocks += self.countBlocks(b.getSubblocks())
            return sumSubblocks + len(blockList)
                

    #assert that the test surveys contain the desired number of blocks
    def testBlockNumber(self):
        print("Running block number test")
        self.assertEqual(self.countBlocks(self.ipierotisSurvey.blockList),3)
        self.assertEqual(self.countBlocks(self.blockSurvey.blockList),9)
        self.assertEqual(self.countBlocks(self.simpleSurvey.blockList),4)
     
    def testAddBlocks(self):
        print("Running add block test")
        block1 = Block([])
        block1.addQuestion(Question("radio","this is a question",[Option("pick me")]))
        block2 = Block([])
        block1.addSubblock(block2)
        self.assertEqual(self.countBlocks([block1]),2)

        #print str(block1)
        self.ipierotisSurvey.addBlock(block1)
        self.blockSurvey.addBlock(block1)
        self.simpleSurvey.addBlock(block1)
        
        self.assertEqual(self.countBlocks(self.ipierotisSurvey.blockList),5)
        self.assertEqual(self.countBlocks(self.blockSurvey.blockList),11)
        self.assertEqual(self.countBlocks(self.simpleSurvey.blockList),6)
        
   

if __name__=='__main__':
    unittest.main()
