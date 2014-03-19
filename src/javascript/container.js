/// <reference path="survey.ts"/>
/// <reference path="block.ts"/>
/// <reference path="question.ts"/>
/// <reference path="option.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />

// functions Containers use
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
    if (exchangeable.length < 2) {
        return blocks;
    }

    // positions that can be swapped into
    var exchangeableIndices = _.map(exchangeable, function (id) {
        var blockids = _.pluck(blocks, 'id');
        return _.indexOf(blockids, id);
    });

    // ids of exchangeable blocks shuffled
    var exchangedIds = _.shuffle(exchangeable);

    // exchangeable blocks in shuffled order
    var exchangedBlocks = _.map(exchangedIds, function (id) {
        return _.filter(blocks, function (b) {
            return b.id === id;
        })[0];
    });

    // pair up each available position with a block
    var pairs = _.zip(exchangeableIndices, exchangedBlocks);

    // fill each position with the block it's paired with
    _.each(pairs, function (pair) {
        blocks[pair[0]] = pair[1];
    });
    return blocks;
}
