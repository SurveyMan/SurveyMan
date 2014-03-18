/// <reference path="survey.ts"/>
/// <reference path="block.ts"/>
/// <reference path="question.ts"/>
/// <reference path="option.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />

function initContainer(container, jsonContainer) {
    jsonContainer = _.defaults(jsonContainer, { exchangeable: [] });
    container.exchangeable = jsonContainer.exchangeable;
    container.contents = makeBlocks(jsonContainer.contents, container);
    container.contents = orderBlocks(container.contents, container.exchangeable);
}

function makeBlocks(jsonBlocks, container) {
    var blockList = _.map(jsonBlocks, function (block) {
        if (block.groups || block.pages) {
            return new InnerBlock(block, container);
        } else {
            return new OuterBlock(block, container);
        }
    });
    return blockList;
}

function orderBlocks(blocks, exchangeable) {
    var exchangeableIndices = _.map(exchangeable, function (id) {
        return _.indexOf(blocks, id);
    });
    var exchangedIds = _.shuffle(exchangeable);
    var exchangedBlocks = _.map(exchangedIds, function (id) {
        return _.filter(blocks, function (b) {
            return b.id === id;
        })[0];
    });
    var pairs = _.zip(exchangeableIndices, exchangedBlocks);
    _.each(pairs, function (pair) {
        blocks[pair[0]] = pair[1];
    });
    return blocks;
}
