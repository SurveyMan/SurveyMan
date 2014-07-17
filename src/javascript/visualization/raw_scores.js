var display_raw_scores = (function(globals) {

    return function () {

        var height = 400,
            width = height,
            trueResponse = function (obj) {
                return obj.q != "q_-1_-1";
            },
            process_data = function (d) {
                return {
                    score : d.score,
                    valid : d.valid,
                    cutoff : d.response.pval,
                    trueResponses : _.filter(JSON.parse(d.response.responses), trueResponse)
                };
            },
            data = _.map(globals.responses, process_data),
            numQs = globals.sm.survey.questions.length,
            maxY = Math.ceil(_.max(_.map(data, function (_d) { return _d.score; }))),
            xInterval = width / (numQs + 1),
            yInterval = height / maxY;


        var svg = d3.select("#resp").append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
          .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        var cValue = function(d) { return d.valid;},
            color = {true : '#FFFF00', false : '#6600FF'};

        // x-axis
        var xValue = function(d) { return d.trueResponses.length; }, // data -> value
            xScale = d3.scale.linear().range([0, width]).domain(_.range(numQs+1)), // value -> display
            xMap = function(d) { console.log(xValue(d), xValue(d)*xInterval); return margin.left + (xValue(d) * xInterval); }, // data -> display
            xAxis = d3.svg.axis().scale(xScale).orient("bottom");

        svg.append("g")
              .attr("class", "x axis")
              .attr("transform", "translate(0," + (height - margin.bottom) + ")")
              .call(xAxis)
            .append("text")
              .attr("class", "label")
              .attr("x", width)
              .attr("y", -6)
              .style("text-anchor", "end")
              .text("Number of Questions Answered");

        //y-axis

        var yValue = function(d) { return d.score;}, // data -> value
            yScale = d3.scale.linear().rangeBands([height, 0]).domain([0, maxY]), // value -> display
            yMap = function(d) { return yValue(d);}, // data -> display
            yAxis = d3.svg.axis().scale(yScale).orient("left");

        svg.append("g")
              .attr("class", "y axis")
              .call(yAxis)
            .append("text")
              .attr("class", "label")
             // .attr("transform", "rotate(-90)")
              .attr("y", 6)
              .attr("dy", ".71em")
              .style("text-anchor", "end")
              .text("Score");

        svg.selectAll("circle")
              .data(data)
            .enter().append("circle")
              .attr("r", 3.5)
              .attr("cx", xMap)
              .attr("cy", yMap)
              .style("fill", function(d) { return color[cValue(d)];});

          // draw legend
        var legend = svg.selectAll(".legend")
              .data(color)
            .enter().append("g")
              .attr("class", "legend")
              .attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; });

          // draw legend colored rectangles
        legend.append("rect")
              .attr("x", width - 18)
              .attr("width", 18)
              .attr("height", 18)
              .style("fill", color);

          // draw legend text
        legend.append("text")
              .attr("x", width - 24)
              .attr("y", 9)
              .attr("dy", ".35em")
              .style("text-anchor", "end")
              .text(function(d) { return d ? "Valid" : "Invalid";});

    };
})(globals);