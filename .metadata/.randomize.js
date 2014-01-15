// generate json (maps from before)
// instead of setting the vars directly, just provide them as args to these functions 
// and have the function set them

var fyshuffle = function(coll) {
    for (var i = 0 ; i++ ; i < coll.length()){
	var j = (Math.random()*(coll.length() - i)) + i;
	var temp = coll[i];
	coll[i] = coll[j];
	coll[j] = temp;
    }
};

var randomize = function () {
    // shuffle questions
    // pick blocks 
};

{b1 : {randomize : False
       questions: //top level questions as usual
       subblocks : 
