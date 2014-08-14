var display_raw_scores = (function(globals) {

    return function () {

        addInstructions($("#resp"),
        "Below are respondents' scores. The X-axis represents the total number of questions answered by a particular respondent. "
        +"The Y-axis represents that respondent's score. "
        +"You can click on individual data points to see a respondent's full answer.");

        var height = 400,
            width = height,
            radius = 5,
            process_data = function (d) {
                return {
                    score : d.score,
                    valid : d.valid,
                    cutoff : d.response.pval,
                    trueResponses : _.filter(JSON.parse(d.response.responses), trueResponse)
                };
            },
            data = _.map(globals.responses, process_data),
            numQs = _.reduce(globals.sm.survey.topLevelBlocks, function (n, b) { return n + b.getAllBlockQuestions().length; }, 0),
            maxY = Math.ceil(_.max(_.map(data, function (_d) { return _d.score; }))),
            minY = Math.floor(_.min(_.map(data, function (_d) { return _d.score; }))),
            xInterval = width / (numQs + 1),
            yInterval = height / maxY,
            numValid = _.filter(globals.responses, function (_d) { return _d.valid; }).length,
            numInvalid = _.filter(globals.responses, function (_d) { return ! _d.valid; }).length;

        console.assert(numValid + numInvalid === globals.responses.length);

        var svg = d3.select("#resp").append("svg")
            .attr("class", "col-md-7")
            .attr("width", width + margin.left + margin.right + 10)
            .attr("height", height + margin.top + margin.bottom)
          .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        var cValue = function(d) { return d.valid; },
            color = function (d) { return {true : 'green', false : 'red'}[cValue(d)]; };

        // x-axis
        var xValue = function(d) { return d.trueResponses.length; }, // data -> value
            xScale = d3.scale.linear().range([0, width]).domain([0, numQs+1]), // value -> display
            xMap = function(d) { return xScale(xValue(d)); }, // data -> display
            xAxis = d3.svg.axis().scale(xScale).orient("bottom");

        svg.append("g")
              .attr("class", "x axis")
              .attr("transform", "translate(0," + (height + 2) + ")")
              .call(xAxis)
            .append("text")
              .attr("class", "label")
              .attr("x", width)
              .attr("y", -6)
              .style("text-anchor", "end")
              .text("Number of Questions Answered");

        //y-axis

        var yValue = function(d) { return d.score;}, // data -> value
            yScale = d3.scale.linear().range([height, 0]).domain([minY, maxY]), // value -> display
            yMap = function(d) { return yScale(yValue(d)); }, // data -> display
            yAxis = d3.svg.axis().scale(yScale).orient("left");

        svg.append("g")
              .attr("class", "y axis")
              .call(yAxis)
            .append("text")
              .attr("class", "label")
              .attr("transform", "rotate(-90)")
              .attr("y", 6)
              .attr("dy", ".71em")
              .style("text-anchor", "end")
              .text("Score");

        var dataPoints = svg.selectAll("circle")
              .data(data)
            .enter().append("circle")
              .attr("r", radius)
              .attr("cx", xMap)
              .attr("cy", yMap)
              .attr("stroke", "black")
              .attr("stroke-width", 1)
              .style("fill", color)
              .attr("cursor", "pointer")
              .on("click", function (d) { zoomResponse(d, "resp"); });

        dataPoints.append("title").text(function (d) { return "(" + xValue(d) + "," + yValue(d) + ")"; });
          // draw legend
        var legend = d3.selectAll("#resp")
            .append("svg")
            .attr("width", 100)
            .attr("height", 100)
            .attr("class", "col-md-2")
            .append("g")
          .attr("transform", "translate(0,0)");


        legend.selectAll(".legend")
              .data([true, false])
            .enter().append("rect")
              .attr("class", "legend")
                .attr("x", 0)
                .attr("y", function (d, i) { return 100 * (i / (i + 1)); })
              .attr("height", 50)
              .attr("width", 100)
              .style("fill", function (d) { return d ? "green" : "red"; })
                          .attr("stroke", "black")
            .attr("stroke-width", 3);


        legend.selectAll(".label")
        .data([true, false]).enter()
            .append("text")
            .attr("x", 50)
            .attr("y", function (d, i) { return 100 * (i / (i + 1)) + 25; })
              .attr("dy", ".35em")
              .attr("class", "text-center")
              .style("text-anchor", "middle")
              .style("font-size", "150%")
              .attr("fill", function (d) { return d ? "white" : "black"; })
               .text(function(d) { return d ? "Valid (" + numValid + ")" : "Invalid (" + numInvalid + ")";});


    };
})(globals);