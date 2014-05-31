// what are we plotting in the correlated graphs?
// one of these graphs per question
// someone clicks on "correlations" on the main page.
// this will show a heatmap (or maybe a hinton diagram) of questions to questions
// you can click on a square in the diagram and see data about the pair
// you can click on a question and see information about the distribution of responses

var heatmap = function (csv_file) {

    d3.csv(csv_file,
        function (d) {
            return {
                q1 : getQuestionById(d.q1),
                q2 : getQuestionById(d.q2),
                corr : +d.corr,
                coeff : d.coeff
            };
        },
        function (error, rows) {

        }
    );

};