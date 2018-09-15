
function numberWithCommas(x) {
    return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

function constructElement(tag, id, classNamesList, text) {
    return "<" + tag + " id=\"" + id + "\" class=\"" + classNamesList.join(" ") + "\">" + text + "</" + tag + ">";
}

function createDiv(id, classNames, text) {
    return constructElement("div", id, classNames, text);
}
