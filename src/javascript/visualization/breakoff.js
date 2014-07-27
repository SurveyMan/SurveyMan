var display_breakoff = (function(globals) {

    return function() {

        addInstructions($("#bkoffs"),
            "The chart below illustrates breakoff by position and question. "
            +"The X-axis represents the total number of questions answered. In the event of no breakoff, all responses will be in the final position. "
            +"The Y-axis represents the total number of respondents who broke off at a particular position. "
            +"<br/><br/>"
            +"Note that there are colored bands in the histogram; these bands represent individual questions. "
            +"If you hover your mouse over a band, you will see the question text and the total number of respondents who broke off at this question <b>and</b> this position. "
            +"If you click on a bands, it will fade all other questions, highlighting the effect of individual questions on breakoff."
        );

        // y axis is position (since we can have very long surveys
        // x axis is count
        var maxPos = _.max(_.pluck(globals.bkoffs, 'pos')),
            maxCt = globals.responses.length, //_.max(_.map(_.range(1, _.max(_.pluck(globals.bkoffs, 'pos'))+1), function (pos) { return _.reduce(_.filter(globals.bkoffs, function(p) { return p.pos ===pos; }), function(a,b) { return a + b.ctValid + b.ctInvalid; }, 0); })),
            unitHeight = 30,
            width = 980,
            unitWidth = width / maxCt,
            axisThickness = 2,
            tickThickness = 1,
            tickLength = 5,
            ctPeriod = 10,
            padding = 30,
            legendHeight = 30,
            legendWidth = 3 * legendHeight,
            height = unitHeight * maxPos,
            processData = function (d) {
                return {
                    q : globals.sm.getQuestionById(d.q),
                    pos : +d.pos,
                    ctValid : +d.ctValid,
                    ctInvalid : +d.ctInvalid
                };
            };

        var qs  = _.uniq(_.pluck(globals.bkoffs, 'q')),
            randCols = _.uniq(_.map(_.range(qs.length), function (foo) { return "hsl(" + ((360 / qs.length) * foo) + ",100%,50%)"; }))
            color = _.object(qs, randCols);

        console.log(qs);
        console.log(randCols);
        console.log(color);

        var svg = d3.select("#bkoffs").append("svg")
            .attr("width", width + margin.left + margin.right + padding)
            .attr("height", height + margin.top + margin.bottom)
            .append("g")
            .attr("transform", "translate("+ margin.left + "," + margin.top + ")");
            
        var axes = svg.append("rect")
            .attr("x", margin.left)
            .attr("y", margin.top)
            .attr("width", width)
            .attr("height", height)
            .attr("fill", "white")
            .attr("stroke-width", axisThickness)
            .attr("stroke", "black");


        var barChart = svg.selectAll("#bkoffs")
            .data(_.map(globals.bkoffs, processData))
            .enter()
            .append("rect")
            .attr("x", function(d, i) {
                    //get all other things at this y
                    var bars_data = _.sortBy(_.filter(globals.bkoffs
                                                      , function(_d) { return _d.pos === d.pos; })
                                              ,  function (a) { return JSON.parse(a.q.split("_")[1]); });
                    //get preceding things at this y -- this assumes we always process in the same order
                    var n = _.indexOf(bars_data
                                      , _.find(bars_data, function (_d) {
                                                            return _d.q === d.q.id
                                                            && _d.pos === d.pos
                                                            && _d.ctInvalid === d.ctInvalid
                                                            && _d.ctValid === d.ctValid;})
                                      ),
                        preceding_entries = _.initial(bars_data, bars_data.length - n);
                    //get the count of preceding things at this y
                    var retval = [];
                    if (preceding_entries.length===0)
                        retval = margin.left;
                    else {
                        var preceeding_data_frequencies = _.pluck(preceding_entries, 'ctValid').concat(_.pluck(preceding_entries, 'ctInvalid'));
                        retval = margin.left + (_.reduce(preceeding_data_frequencies
                                                            , function (a, b) { return a + b; }
                                                            , 0)
                                                * unitWidth);
                    }
                    return retval;
                })
            .attr("y", function(d, i) { return ((d.pos - 1) * unitHeight) + margin.top; })
            .attr("width", function (d) { return unitWidth * (d.ctInvalid  + d.ctValid); })
            .attr("height", unitHeight)
            .attr("stroke", "black")
            .attr("stroke-width", 1)
            .style("fill", function (d, i) { return color[d.q.id]; })
            .attr("cursor", "pointer")
            .on("click", function (a) {
                d3.select("#bkoffs")
                    .selectAll("rect").attr("fill-opacity", function (b) {
                        if (_.isUndefined(b) || a.q === b.q)
                            return 1.0;
                        else return 0.2;
                });
            });


        barChart.append("title")
            .text(function (d) {console.log(d); return d.q.qtext + " (" + d.q.id + ")" + "\n# broke off: " + (d.ctInvalid + d.ctValid);});

 
        var x = _.map(_.range(1,Math.floor(maxCt / ctPeriod)+1), function (d) { return d * ctPeriod;})

        svg.selectAll(".xTick")
            .data(x)
            .enter()
            .append("line")
            .attr("x1", function (d) { return margin.left + (d * unitWidth); })
            .attr("x2", function (d) { return margin.left + (d * unitWidth); })
            .attr("y1", margin.top - tickLength)
            .attr("y2", margin.top + (unitHeight * maxPos))
            .attr("stroke-width", tickThickness)
            .attr("stroke", "black");

        svg.selectAll(".xLabel")
            .data(x)
            .enter()
            .append("text")
            .text(function (d) { return "" + d;})
            .attr("x", function (d) { return margin.left + (d * unitWidth); })
            .attr("y", 0)
            .attr("text-anchor", "middle")
            .style("font-family", "sans-serif");


        // set ticks for every five respondents

        svg.selectAll(".yTick")
            .data(_.range(maxPos))
            .enter()
            .append("line")
            .attr("x1", margin.left - tickLength)
            .attr("y1", function (d) { return margin.top + unitHeight/2 + (d * unitHeight); })
            .attr("x2", margin.left)
            .attr("y2", function (d) { return margin.top + (unitHeight/2) + (d * unitHeight); })
            .attr("stroke-width", tickThickness)
            .attr("stroke", "black");

        svg.selectAll(".yLabel")
            .data(_.range(maxPos))
            .enter()
            .append("text")
            .text(function (d) { return "" + (d+1); })
            .attr("x", 0)
            .style("text-anchor", "end")
            .style("font-family", "sans-serif")
            .attr("y", function (d) { return margin.top + (unitHeight/2) + (unitHeight*d) });


    };
})(globals);