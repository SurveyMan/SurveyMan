#!/bin/bash
# make sure to set params.properties sandbox to false
lein run -m report --report=dynamic --hits=2GZHRRLFQ0OT6OX3OP8OKEGRDBJHQ4,3N7PQ0KLI51X8QZZ5Y3V9BR4UH13EK,3E9ZFLPWOY4L4T8ZL60A16E0T3MIXH --payBonus=true --results=data/results/wage_survey.csv --backend=MTURK data/samples/wage_survey.csv
