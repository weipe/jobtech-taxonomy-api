// Yikes! An unhealthy load of global variables.
var radius = 5; // circle size
var duration = 750; // transition time in millisec

// graph globals
var tree = '';
var diagonal = '';
var svg = '';
var root = '';


// node counter - to estimate canvas size
function recCount(dep, jsonObj) {
    var count = 0;

    if (jsonObj !== null && typeof jsonObj == "object") {
        Object.entries(jsonObj).forEach(([key, value]) => {
            count = count + recCount(dep + 1, value);
        });
    } else {
        count = 1;
    }
    return count;
}


// experimental, to optionally start graph in collapsed state
function collapse(d) {
    if (d.children) {
        d._children = d.children;
        d.children.forEach(collapse);
        d.children = null;
    }
}


// trigged by form submit button
function renderGraph(relationType, useCache) {

    toggleLoader(1);

    // cleanse out any existing junk.
    d3.selectAll("svg > *").remove();
    
    var resource = "/relation/graph?relation-type=" + relationType;

    if(useCache) {
        resource = "cached_" + relationType + ".json";
    }

    d3.json(resource, function (error, r) {

        var margin = { top: 20, right: 120, bottom: 20, left: 120 };
        var width = document.body.clientWidth;
        var height = recCount(0, r) * 9;

        toggleLoader(0);

        tree = d3.layout.tree()
            .size([height, width]);

        diagonal = d3.svg.diagonal()
            .projection(function (d) { return [d.y, d.x]; });

        svg = d3.select("body").append("svg")
            .attr("width", width + margin.right + margin.left)
            .attr("height", height + margin.top + margin.bottom)
            .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        root = r;
        root.x0 = height / 2;
        root.y0 = 0;

        update(root);

        // start collapsed:
        //collapse(root);
        //update(root);

        d3.select(self.frameElement).style("height", "500px");

        // scroll to root node
        window.scrollTo(0, Math.trunc((document.body.clientHeight - 100) / 2));
    })
}


// generic D3 stuff
function update(currentNode) {

    var i = 0; // please ponder the meaning of this variable.

    // Compute the new tree layout.
    var nodes = tree.nodes(root).reverse(),
        links = tree.links(nodes);

    // Normalize for fixed-depth.
    nodes.forEach(function (d) { d.y = d.depth * (document.body.clientWidth/5); });

    // Update the nodes…
    var node = svg.selectAll("g.node")
        .data(nodes, function (d) { return d.id || (d.id = ++i); });

    // Enter any new nodes at the parent's previous position.
    var nodeEnter = node.enter().append("g")
        .attr("class", "node")
        .attr("transform", function (d) { return "translate(" + currentNode.y0 + "," + currentNode.x0 + ")"; })
        .on("click", click);

    nodeEnter.append("circle")
        .attr("r", radius)
        .style("fill", function (d) { return d._children ? "lightsteelblue" : "#fff"; });

    nodeEnter.append("text")
        .attr("x", function (d) { return d.children || d._children ? -13 : 13; })
        .attr("dy", ".35em")
        .attr("text-anchor", function (d) { return d.children || d._children ? "end" : "start"; })
        .text(function (d) { return d.name; })
        .style("fill-opacity", 1e-6);

    // Transition nodes to their new position.
    var nodeUpdate = node.transition()
        .duration(duration)
        .attr("transform", function (d) { return "translate(" + d.y + "," + d.x + ")"; });

    nodeUpdate.select("circle")
        .attr("r", radius)
        .style("fill", function (d) { return d._children ? "lightsteelblue" : "#fff"; });

    nodeUpdate.select("text")
        .style("fill-opacity", 1);

    // Transition exiting nodes to the parent's new position.
    var nodeExit = node.exit().transition()
        .duration(duration)
        .attr("transform", function (d) { return "translate(" + currentNode.y + "," + currentNode.x + ")"; })
        .remove();

    nodeExit.select("circle")
        .attr("r", radius);

    nodeExit.select("text")
        .style("fill-opacity", 1e-6);

    // Update the links…
    var link = svg.selectAll("path.link")
        .data(links, function (d) { return d.target.id; });

    // Enter any new links at the parent's previous position.
    link.enter().insert("path", "g")
        .attr("class", "link")
        .attr("d", function (d) {
            var o = { x: currentNode.x0, y: currentNode.y0 };
            return diagonal({ source: o, target: o });
        });

    // Transition links to their new position.
    link.transition()
        .duration(duration)
        .attr("d", diagonal);

    // Transition exiting nodes to the parent's new position.
    link.exit().transition()
        .duration(duration)
        .attr("d", function (d) {
            var o = { x: currentNode.x, y: currentNode.y };
            return diagonal({ source: o, target: o });
        })
        .remove();

    // Stash the old positions for transition.
    nodes.forEach(function (d) {
        d.x0 = d.x;
        d.y0 = d.y;
    });
}

// Toggle children on click.
function click(d) {
    if (d.children) {
        d._children = d.children;
        d.children = null;
    } else {
        d.children = d._children;
        d._children = null;
    }
    update(d);
}
