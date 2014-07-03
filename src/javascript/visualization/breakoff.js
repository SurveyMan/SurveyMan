var makeBarchart = function(bkoffs, sm) {
    // y axis is position (since we can have very long surveys
    // x axis is count
    var maxPos = sm.survey.questions.length,
        maxCt = 150,
        margin = { top : 15, right : 0, bottom : 0, left: 5},
        width = 960,
        height = width,
        unitHeight = 30,
        unitWidth = 10,
        padding = 5,
        processData = function (d) {
            return {
                q : sm.getQuestionById(d.q),
                valid : JSON.parse(d.valid),
                pos : +d.pos,
                ct : +d.ct
            };
        };

    var color = _.map(bkoffs, function(_) {
            return "hsl(" + Math.random() * 360 + ",100%,50%)";
            });

    var svg = d3.select("#bkoffs").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.left + margin.right)
        .append("g")
        .attr("transform", "translate("+ margin.left + "," + margin.top + ")");



    var xAxis = d3.svg.axis()
        //.scale(d3.scale.ordinal().rangeRoundBands([0, maxCt], 1))
        .orient("top");

    var barChart = svg.selectAll("#bkoffs")
        .data(_.map(bkoffs, processData))
        .enter()
        .append("rect")
        .attr("x", function(d, i) {
                //get all other things at this y
                var this_bars_data = _.sortBy(_.filter(bkoffs, function(_d) {
                    return _d.pos === d.pos; }), function(__d) {
                        return __d.q;
                    });
                //get preceding things at this y -- this assumes we always process in the same order
                var preceding_entries = _.initial(this_bars_data
                    , this_bars_data.length - _.indexOf(this_bars_data,
                        { q : d.q.id, valid : d.valid, pos : d.pos, ct : d.ct }));
                //get the count of preceding things at this y
                var retval = [];
                if (preceding_entries.length===0)
                    retval = margin.left;
                else {
                    var preceeding_data_frequencies = _.pluck(preceding_entries, 'ct');
                    retval = margin.left + (_.reduce(preceeding_data_frequencies
                                                        , function (a, b) { return a + b; }
                                                        , 0)
                                            * unitWidth);
                }
                return retval;
            })
        .attr("y", function(d, i) { return (d.pos * (unitHeight + padding)); })
        .attr("width", function (d) { return unitWidth * d.ct; })
        .attr("height", unitHeight)
        .style("fill", function (d, i) { return color[i]; });

    svg.append("g")
        .attr("class", "x axis")
        .call(xAxis);

    barChart.append("title")
        .text(function (d) {return d.q.qtext + "(" + d.q.id + ")" + "\n# broke off: " + d.ct;});
};