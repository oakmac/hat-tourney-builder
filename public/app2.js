function byId (id) {
  return document.getElementById(id)
}




function htmlEscape (s) {
  // TODO: write me
  return s
}


const chris1 = {
  id: "chrisoakman",
  name: "Chris Oakman",
  sex: "male"
}

const lauren1 = {
  id: 'laurenoakman',
  name: 'Lauren Oakman',
  sex: 'female'
}

let players = new Map()
players.set('chrisoakman', chris1)
players.set('laurenoakman', lauren1)


function buildPlayerBox (player) {
  let html = '<div '
  html += 'id="' + player.id + '" '
  html += 'class="player-box '

  if (player.sex === 'male') html += 'sex-male">'
  if (player.sex === 'female') html += 'sex-female">'

  html += htmlEscape(player.name)
  html += '</div>'

  return html
}


var col1 = byId('col1')
var col2 = byId('col2')
var col3 = byId('col3')

var linkBox = byId('linkBox')
var unlinkBox = byId('unlinkBox')

new Sortable(col1, {
    group: 'shared',
    animation: 150
});

new Sortable(col2, {
    group: 'shared',
    animation: 150
});

new Sortable(col3, {
    group: 'shared',
    animation: 150
});



new Sortable(linkBox, {
    onAdd: onAddLinkBox,
    onEnd: onEndLinkBox,
    group: 'shared',
    animation: 150
});

new Sortable(unlinkBox, {
    group: 'shared',
    animation: 150
});

function onEndLinkBox (evt) {
    // var itemEl = evt.item;  // dragged HTMLElement
    // evt.to;    // target list
    // evt.from;  // previous list
    // evt.oldIndex;  // element's old index within old parent
    // evt.newIndex;  // element's new index within new parent
    // evt.oldDraggableIndex; // element's old index within old parent, only counting draggable elements
    // evt.newDraggableIndex; // element's new index within new parent, only counting draggable elements
    // evt.clone // the clone element
    // evt.pullMode;  // when item is in another sortable: `"c

  const itemId = evt.item

  console.log('onEndLinkBox event:')
  console.log(itemId)
}

function onAddLinkBox (evt) {
    // var itemEl = evt.item;  // dragged HTMLElement
    // evt.to;    // target list
    // evt.from;  // previous list
    // evt.oldIndex;  // element's old index within old parent
    // evt.newIndex;  // element's new index within new parent
    // evt.oldDraggableIndex; // element's old index within old parent, only counting draggable elements
    // evt.newDraggableIndex; // element's new index within new parent, only counting draggable elements
    // evt.clone // the clone element
    // evt.pullMode;  // when item is in another sortable: `"c

  const itemId = evt.item

  const players = getPlayersInLinkBox()
  console.log('Players in Link Box:')
  console.log(players)

  const linkBoxEl2 = byId('linkBox')
  console.log('00000000000000000')
  console.log(linkBoxEl2.innerHTML)

  linkBoxEl2.innerHTML = '<div class="player-group">' + buildPlayerBox(chris1) +
    buildPlayerBox(lauren1) + buildPlayerBox(chris1) + '</div>'
}

function getPlayersInLinkBox () {
  const playerEls = document.querySelectorAll('#linkBox .player-box')

  let playerIds = []
  playerEls.forEach((el) => playerIds.push(el.id))

  return playerIds
}
