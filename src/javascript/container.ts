
/// <reference path="survey.ts"/>
/// <reference path="block.ts"/>
/// <reference path="question.ts"/>
/// <reference path="option.ts"/>
/// <reference path="node_modules/jquery/jquery.d.ts" />
/// <reference path="node_modules/underscore/underscore.d.ts" />


// A Container contains Blocks, so Surveys and OuterBlocks are Containers.
interface Container{
    exchangeable;
    contents;

    advance();
}

function initContainer(container: Container, jsonContainer){
    jsonContainer = _.defaults(jsonContainer, {exchangeable: []});
    container.exchangeable = jsonContainer.exchangeable;
    container.contents = makeBlocks(jsonContainer.contents, container);
    container.contents = orderBlocks(container.contents, container.exchangeable);
}

function makeBlocks(jsonBlocks, container: Container): Block[] {
    var blockList = _.map(jsonBlocks, (block):Block => {
            if (block.groups || block.pages){
                return new InnerBlock(block, container);
            } else {
                return new OuterBlock(block, container);
            }
        });
    return blockList;
}

function orderBlocks(blocks: Block[], exchangeable: string[]): Block[] {
    var exchangeableIndices: number[] = _.map(exchangeable, (id: string):number => {
                                                  return _.indexOf(blocks, id)
                                              });
    var exchangedIds: number[] = _.shuffle<number>(exchangeable);
    var exchangedBlocks = _.map(exchangedIds, (id:string):Block => {
                                    return _.filter(blocks, (b:Block):boolean => {
                                        return b.id === id
                                    })[0]
                                });
    var pairs = _.zip(exchangeableIndices, exchangedBlocks);
    _.each(pairs, (pair:any[]):void => {blocks[pair[0]] = pair[1]});
    return blocks;
}

