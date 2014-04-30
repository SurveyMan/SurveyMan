Surveys are passed between the runtime system and the backend via JSON. The JSON specification is listed below. Since payload size is a concern, if a field is not needed, it is omitted and default values are supplied. The full JSON schema is located in [resources/survey.csv

### Top Level SurveyMan JSON
The top level of the JSON has one required key, and two optional keys. 
* **survey** : The required key is "survey." This will be the JSON representation of the survey itself.
* **filename** : This refers to the local file name of the survey source. It is a string that is a convenience for retreiving and parsing survey data.
* **breakoff** : This is a boolean valued variable that is true iff there exists a question in the survey that permits breakoff. It will trigger a notice informing the respondent that she will be allowed to submit her results early at some point in the survey.

An example top-level view of the JSON for a survey might be :
````
{
	"filename" : "/home/johnsmith/mysurvey.csv",
	"breakoff" : false
	"survey" : < survey JSON > 
}
````

### Survey JSON
A survey is a list/array of Block JSON objects. If a survey has no blocks, then all questions should be members of one top-level block. 

From the example above, we have
````
{
	"filename" : "/home/johnsmith/mysurvey.csv",
	"breakoff" : false
	"survey" : [ < block 1 JSON > , ... , < block n JSON > ]  
}
````

### Block JSON
A block is a logical unit of a survey. Blocks can be movable or fixed, and so they may participate in randomization. A block has two required keys: the identifier, and at least one of a nonempty "questions" list or a nonempty "subblocks" list. The supported keys are as follows :
* **id** : The block identifier. This should be the literal block identifier from a source csv (e.g. 1.2.3) and is a string.
* **questions** : A list/array of the top-level questions that belong directly to this block. This key is not required if the subblocks key is present and if the subblocks value is a non-empty list. In that case, this block is a container for subblocks.
* **subblocks** : A list/array of the child blocks of this subblock. If this field does not exist or if its value is the empty list, then the "questions" key must be present and be a non-empty list.
* **randomize** : A boolean valued flag indicating whether a block should be randomizable.

As an example, an entry in the block list that is the value corresponding to the survey key above might look like this:

````
{
	"id" : "1",
	"questions" : [ < question 1 JSON > , ... , < question m JSON > ],
	"randomize" : false,
	"subblocks" : [{ "id" : "1.1", "questions" : ...
}
````

### Question JSON
Questions are maps with two required keys.
* **id** : The question identifier is a string. If the original survey was loaded from a csv, the identifier will be of the form `q_x_y`, where `x` is the question's row number and `y` is the question's column. This key is required.
* **qtext** : This is a string that corresponds to the text to be displayed and is required. It may contain arbitrary HTML.
* **options** : This is a list/array of Option JSON objects. If this list is left blank, no options are displayed.
* **branchMap** : This is a map from Option ids to Branch ids and is not required.
* **freetext** : Boolean-valued flag inidcating that a question's response should be a freetext area. When this flag is true, the options key should either not be present or should be an empty list. If this key is not provided, the default value is set to false.
* **randomize** : Boolean-valued flag indicating that the options for this question should be randomized. If the key is not provided, the default vaue is set to true.
* **ordered** : Boolean-valued flag indicating that the options for this question are ordered. If the key is not provided, the default value is set to false.
* **exclusive** : Boolean-valued flag that when true indicates that this question should be displayed with radio buttons. When false, the options are displayed as checkboxes. The default value is set to true.
* **permitBreakoff** : Boolean-valued flag indicating that the respondent may submit the survey at this point (even if this question is not the last question). The default value is false.

An example Question JSON is :
````
{ 
	"id" : "q_5_1", 
  	"qtext" : "Which dairy products do you prefer?",
  	"options" : [< option 1 JSON > , ... , < option p JSON >],
  	"branchMap : { "comp_5_2" : "2", "comp_6_2" : "3" },
  	"permitBreakoff" : true
}
````

### Option JSON
An option is just a map of an id and text. All option display information is held in the Question JSON. If the original survey was loaded in from csv, the idenfier should be of the form `comp_x_y`, where `x` refers to the row of the option and `y` refers to the column of the option. An example option might be :

````
{ "id" : "comp_3_2", "otext" : "Yes, I do like cheese." }
````
