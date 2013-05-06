# given a Survey object
# post survey, according to the format given (questions in the order they're provided, options in the order they're provided)
# get survey answers
# function to return survey answers as responses (list of tuples of question object and option list)

# so basically, continually run on some port provided as input. 
# process get and post requests
# note : if you want to post each quesiton as a html file, you will need to keep cookies to maintain your session
# another way to handle this is to write javascript to update the page
# if you go the static web page route, then you need to accumulate answers as the person goes. 
# if you respond to user input indicating that a question has been answered, then you can get all of the answers back at once
# while the former is easier to code up in the short term, it presents problems in terms of session management (we will definitely only be able to hand one survey taker at a time)
# the JS approach might be more of a pain, but it's possible that there are python libraries that compile python to JS
