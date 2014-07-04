var makeBarchart = function(bkoffs, sm) {
    // y axis is position (since we can have very long surveys
    // x axis is count
    var maxPos = _.max(_.pluck(bkoffs, 'pos')),
        maxCt = _.max(_.map(_.range(1, _.max(_.pluck(bkoffs, 'pos'))+1), function (pos) { return _.reduce(_.filter(bkoffs, function(p) { return p.pos ===pos; }), function(a,b) { console.log(a,b); return a + b.ctValid + b.ctInvalid; }, 0); })),
        margin = { top : 15, right : 0, bottom : 0, left: 5},
        width = 960,
        height = width,
        unitHeight = 30,
        unitWidth = width / maxCt,
        axisThickness = 2,
        tickThickness = 1,
        processData = function (d) {
            return {
                q : sm.getQuestionById(d.q),
                pos : +d.pos,
                ctValid : +d.ctValid,
                ctInvalid : +d.ctInvalid
            };
        };

    var qs  = _.uniq(_.pluck(bkoffs, 'q')),
        randCols = _.uniq(_.map(_.range(qs.length), function (foo) { return "hsl(" + ((360 / qs.length) * foo) + ",100%,50%)"; }))
        color = _.object(qs, randCols);

    console.assert(qs.length===randCols.length);
    console.log(color);

    var svg = d3.select("#bkoffs").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.left + margin.right)
        .append("g")
        .attr("transform", "translate("+ margin.left + "," + margin.top + ")");

    var xAxis = svg.append("line")
        .attr("x1", margin.left)
        .attr("y1", margin.top)
        .attr("x2", width + margin.left)
        .attr("y2", margin.top)
        .attr("stroke-width", axisThickness)
        .attr("stroke", "black")

    // set ticks for every five respondents

    var yAxis = svg.append("line")
        .attr("x1", margin.left)
        .attr("y1", margin.top)
        .attr("x2", margin.left)
        .attr("y2", margin.top + (unitHeight * maxPos))
        .attr("stroke-width", axisThickness)
        .attr("stroke", "black");


    var barChart = svg.selectAll("#bkoffs")
        .data(_.map(bkoffs, processData))
        .enter()
        .append("rect")
        .attr("x", function(d, i) {
                //get all other things at this y
                var bars_data = _.sortBy(_.filter(bkoffs, function(_d) {
                                                return _d.pos === d.pos; })
                                              ,  function (a) {
                                                    return JSON.parse(a.q.split("_")[1]);
                                              });
                //get preceding things at this y -- this assumes we always process in the same order
                var n = _.indexOf(bars_data, _.find(bars_data, function (_d) {
                            return _d.q === d.q.id
                            && _d.pos === d.pos
                            && _d.ctInvalid === d.ctInvalid
                            && _d.ctValid === d.ctValid;
                        })),
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
                return retval + axisThickness;
            })
        .attr("y", function(d, i) { return (d.pos * unitHeight); })
        .attr("width", function (d) { return unitWidth * (d.ctInvalid  + d.ctValid); })
        .attr("height", unitHeight)
        .style("fill", function (d, i) { return color[d.q.id]; });


    barChart.append("title")
        .text(function (d) {return d.q.qtext + " (" + d.q.id + ")" + "\n# broke off: " + (d.ctInvalid + d.ctValid);});
};