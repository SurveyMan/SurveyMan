function getQuestionHTML (q) {

    var div = document.createElement("div");
    //    div.id = q.id;
    $(div).attr({"class" : "col-md-6"});
    $(div).css("margin-top" , "10px");
    $(div).css("padding-right", "10px");
    //$(div).css("border", "solid");
    $(div).append("<p class='text-center'>" + q.qtext + "</p>");
    //$(div).append(sm.getOptionHTML(q));
    makeResponseChart(q, getResponseCounts(q), div);
    return div;

};

function getJsonizedResponses () {
    return _.map(globals.responses, function (r) { return JSON.parse(r.response.responses); });
};

function getResponseCounts (q) {
    // return a map of options to counts
    var jsonizedResponses = getJsonizedResponses(),
        responsesForQ = _.map(jsonizedResponses, function (sr) { return _.filter(sr, function (r) { return r.q===q.id;})})
    return _.filter(responsesForQ, function (rfq) { return rfq.length > 0 && rfq[0].hasOwnProperty("opts"); });
};

function makeResponseChart (q, responseMap, targetDiv) {

    var margin = {top : 30, left: 25, bottom : 0, right : 40},
        ct = responseMap.length,
        indexLookup = _.map(q.options, function (o) { return o.id; }),
        svgWidth = 350,
        barWidth = svgWidth / q.options.length - 5,
        ctHeight = 10,
        axisThickness = 2,
        tickThickness = 1
        tickInterval = 5,
        tickLength = 5,
        color = _.uniq(_.map(_.range(q.options.length), function (foo) { return "hsl(" + ((360 / q.options.length) * foo) + ",100%,50%)"; })),
        data = _.flatten(_.map(q.options, function (option) {
                                                return _.map(_.range(q.options.length), function (i) {
                                                                                            return {
                                                                                                id : option.id,
                                                                                                text : option.otext,
                                                                                                index : i,
                                                                                                ct : _.filter(responseMap, function (m) {
                                                                                                                            var opt = m[0].opts[0];
                                                                                                                            return opt.o===option.id && opt.oindex===i;}).length
                                                                                            };
                                                                                        });
                                           })),
        svgHeight = ct * ctHeight;



    var svg = d3.select(targetDiv).append("svg")
        .attr("width", svgWidth + margin.left + margin.right)
        .attr("height", svgHeight + margin.top + margin.bottom)
        .append("g")
        .attr("transform", "translate("+ margin.left + "," + margin.top + ")");

    var bars = svg.selectAll("rect")
        .data(data)
        .enter()
        .append("rect")
        .attr("x", function (d, i) { return margin.left + _.indexOf(indexLookup, d.id) * barWidth; })
        .attr("y", function (d, i) {
                        return margin.top + (_.reduce(_.filter(data, function (_d) { return d.id === _d.id && _d.index < d.index; })
                                              , function (n, o) { return n + o.ct; }
                                              , 0) * ctHeight);
                   })
        .attr("height", function (d, i) { return d.ct * ctHeight; })
        .attr("width", barWidth)
        .attr("fill", function(d, i) { return color[d.index]; })
        .attr("stroke", "black")
        .attr("stroke-width", tickThickness);

    bars.append("title")
        .text(function (d) { return d.text + " (index: " + d.index + ", count: " + d.ct + ")"; });

    var xAxis = svg.append("line")
        .attr("x1", margin.left)
        .attr("y1", margin.top)
        .attr("x2", margin.left + svgWidth)
        .attr("y2", margin.top)
        .attr("stroke-width", axisThickness)
        .attr("stroke", "black");

    var x = _.map(q.options, function (option) { return option.otext; });

    var xLab = svg.selectAll(".xLabel")
        .data(x)
        .enter()
        .append("text")
        .text(function (d) { return d;})
        .style("text-anchor", "start")
        .attr("transform", "rotate(-15)")
//        .attr("x", function (d, i) { return margin.left + (i * barWidth); })
//        .attr("y", function (d, i) { return margin.top; });
        .attr("x", function (d, i) { return margin.left + (i * barWidth) + (Math.pow(i, 2) * Math.cos(15));} )
        .attr("y", function (d, i) { return margin.top + (i * Math.sin(15) * margin.top) + (Math.pow(i, 2) * Math.sin(15));})

    var yAxis = svg.append("line")
        .attr("x1", margin.left)
        .attr("x2", margin.left)
        .attr("y1", margin.top)
        .attr("y2", svgHeight+margin.top)
        .attr("stroke-width", axisThickness)
        .attr("stroke", "black");

    var y = _.rest(_.map(_.range(Math.floor((svgHeight / ctHeight) / tickInterval)), function (n) { return n * tickInterval; }));

   svg.selectAll(".yTick")
        .data(y)
        .enter()
        .append("line")
        .attr("x1", margin.left - tickLength)
        .attr("y1", function (d, i) { return margin.top + ((i + 1) * tickInterval * ctHeight); })
        .attr("x2", margin.left)
        .attr("y2", function (d, i) { return margin.top + ((i + 1) * tickInterval * ctHeight); })
        .attr("stroke-width", tickThickness)
        .attr("stroke", "black");


   var yLab = svg.selectAll(".yLabel")
        .data(y)
        .enter()
        .append("text")
        .text(function (d) { return d; })
        .style("text-anchor", "end")
        .attr("x", 0)
        .attr("y", function (d, i) { return margin.top + ((i + 1) * tickInterval * ctHeight); });

};