ko.templateSources.domElement.prototype['text'] = function (/* valueToWrite */) {
    var domElementName;
    if (this.domElement) {
        domElementName = 'domElement';
    } else {
        // ko.templateSources.domElement method sets property
        // domElement of this to provided value - let's use it to
        // find out the obfuscated name of the domElement property
        var tmp = {};
        ko.templateSources.domElement.call(tmp, "ahoj");
        for (let p in tmp) {
            if (tmp[p] === "ahoj") {
                domElementName = p;
                break;
            }
        }
    }
    
    var el = this[domElementName];
    var tagNameLower = el.tagName.toLowerCase(),
            elemContentsProperty = tagNameLower === 'script' ? 'text'
            : tagNameLower === 'textarea' ? 'value'
            : 'innerHTML';
    
    if (arguments.length === 0) {
        var val = el[elemContentsProperty];
        if (tagNameLower === 'script' && !val && el['src']) {
            val = ko.observable('Loading...<br/>');
            var xhr = new XMLHttpRequest();
            xhr.open('GET', el['src'], true);
            xhr.setRequestHeader('Content-Type', 'text/html; charset=utf-8');
            xhr.onreadystatechange = function () {
                if (xhr.readyState !== 4)
                    return;
                val(el[elemContentsProperty] = xhr.response || xhr.responseText);
            };
            xhr.onerror = function (e) {
                val(el[elemContentsProperty] = 'Cannot load: ' + e);
            };
            xhr.send();
        }
        return ko.utils.unwrapObservable(val);
    } else {
        var valueToWrite = arguments[0];
        if (elemContentsProperty === 'innerHTML')
            ko.utils.setHtml(el, valueToWrite);
        else
            el[elemContentsProperty] = valueToWrite;
    }
};
