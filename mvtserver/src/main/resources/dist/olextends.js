/*!
 * 
 *     iclient9-openlayers.(http://iclient.supermap.io)
 *     Copyright© 2000-2017 SuperMap Software Co. Ltd
 *     license: Apache-2.0
 *     version: v9.0.1
 * 
 */
/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, {
/******/ 				configurable: false,
/******/ 				enumerable: true,
/******/ 				get: getter
/******/ 			});
/******/ 		}
/******/ 	};
/******/
/******/ 	// define __esModule on exports
/******/ 	__webpack_require__.r = function(exports) {
/******/ 		Object.defineProperty(exports, '__esModule', { value: true });
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = 1);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ (function(module, exports) {

module.exports = ol;

/***/ }),
/* 1 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
    value: true
});
exports.olExtends = undefined;

var _openlayers = __webpack_require__(0);

var _openlayers2 = _interopRequireDefault(_openlayers);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

//applyStyleFunction 调用了ol.geom.LineString.getFlatMidpoint但是该方法在ol-debug.js里才有
var olExtends = exports.olExtends = function olExtends(targetMap) {

    window.targetMapCache = targetMap;
    _openlayers2.default.format.MVT.prototype.readProjection = function (source) {
        // eslint-disable-line no-unused-vars
        return new _openlayers2.default.proj.Projection({
            code: '',
            units: _openlayers2.default.proj.Units.TILE_PIXELS
        });
    };
    if (!_openlayers2.default.geom.LineString.getFlatMidpoint) {
        _openlayers2.default.geom.LineString.prototype.getFlatMidpoint = function () {
            return this.getCoordinateAt(0.5);
        };
    }
    _openlayers2.default.render.canvas.Replay.prototype.applyFill = function (state, geometry) {
        // eslint-disable-line no-unused-vars
        var fillStyle = state.fillStyle;
        var fillInstruction = [_openlayers2.default.render.canvas.Instruction.SET_FILL_STYLE, fillStyle];
        if (typeof fillStyle !== 'string') {
            var viewExtent = window.targetMapCache.getView().getProjection().getExtent();
            fillInstruction.push([viewExtent[0], viewExtent[3]]);
        }
        this.instructions.push(fillInstruction);
    };
    _openlayers2.default.format.MVT.prototype.createFeature_ = function (pbf, rawFeature, opt_options) {
        var type = rawFeature.type;
        if (type === 0) {
            return null;
        }

        var feature;
        var id = rawFeature.id;
        var values = rawFeature.properties;
        values[this.layerName_] = rawFeature.layer.name;

        var flatCoordinates = [];
        var ends = [];
        _openlayers2.default.format.MVT.readRawGeometry_(pbf, rawFeature, flatCoordinates, ends);

        var geometryType = _openlayers2.default.format.MVT.getGeometryType_(type, ends.length);

        if (this.featureClass_ === _openlayers2.default.render.Feature) {
            feature = new this.featureClass_(geometryType, flatCoordinates, ends, values, id);
        } else {
            var geom;
            if (geometryType == _openlayers2.default.geom.GeometryType.POLYGON) {
                var endss = [];
                var offset = 0;
                var prevEndIndex = 0;
                for (var i = 0, ii = ends.length; i < ii; ++i) {
                    var end = ends[i];
                    if (!_openlayers2.default.geom.flat.orient.linearRingIsClockwise(flatCoordinates, offset, end, 2)) {
                        endss.push(ends.slice(prevEndIndex, i + 1));
                        prevEndIndex = i + 1;
                    }
                    offset = end;
                }
                if (endss.length > 1) {
                    ends = endss;
                    geom = new _openlayers2.default.geom.MultiPolygon(null);
                } else {
                    geom = new _openlayers2.default.geom.Polygon(null);
                }
            } else {
                geom = geometryType === _openlayers2.default.geom.GeometryType.POINT ? new _openlayers2.default.geom.Point(null) : geometryType === _openlayers2.default.geom.GeometryType.LINE_STRING ? new _openlayers2.default.geom.LineString(null) : geometryType === _openlayers2.default.geom.GeometryType.POLYGON ? new _openlayers2.default.geom.Polygon(null) : geometryType === _openlayers2.default.geom.GeometryType.MULTI_POINT ? new _openlayers2.default.geom.MultiPoint(null) : geometryType === _openlayers2.default.geom.GeometryType.MULTI_LINE_STRING ? new _openlayers2.default.geom.MultiLineString(null) : null;
            }
            geom.setFlatCoordinates(_openlayers2.default.geom.GeometryLayout.XY, flatCoordinates, ends);
            feature = new this.featureClass_();
            if (this.geometryName_) {
                feature.setGeometryName(this.geometryName_);
            }
            var geometry = _openlayers2.default.format.Feature.transformWithOptions(geom, false, this.adaptOptions(opt_options));
            feature.setGeometry(geometry);
            feature.setId(id);
            feature.setProperties(values);
        }

        return feature;
    };

    _openlayers2.default.geom.flat.textpath.lineString = function (flatCoordinates, offset, end, stride, text, measure, startM, maxAngle) {
        var result = [];

        // Keep text upright
        var anglereverse = Math.atan2(flatCoordinates[end - stride + 1] - flatCoordinates[offset + 1], flatCoordinates[end - stride] - flatCoordinates[offset]);
        var reverse = anglereverse < -0.785 || anglereverse > 2.356; //0.785//2.356
        var isRotateUp = anglereverse < -0.785 && anglereverse > -2.356 || anglereverse > 0.785 && anglereverse < 2.356;

        var numChars = text.length;

        var x1 = flatCoordinates[offset];
        var y1 = flatCoordinates[offset + 1];
        offset += stride;
        var x2 = flatCoordinates[offset];
        var y2 = flatCoordinates[offset + 1];
        var segmentM = 0;
        var segmentLength = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));

        while (offset < end - stride && segmentM + segmentLength < startM) {
            x1 = x2;
            y1 = y2;
            offset += stride;
            x2 = flatCoordinates[offset];
            y2 = flatCoordinates[offset + 1];
            segmentM += segmentLength;
            segmentLength = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        }
        var interpolate = (startM - segmentM) / segmentLength;
        var x0 = _openlayers2.default.math.lerp(x1, x2, interpolate); //起始点
        var y0 = _openlayers2.default.math.lerp(y1, y2, interpolate); //起始点

        var chunk = '';
        var chunkLength = 0;
        var data, index, previousAngle, previousLang;
        for (var i = 0; i < numChars; ++i) {
            index = reverse ? numChars - i - 1 : i;
            var char = text.charAt(index);
            var charcode = char.charCodeAt(0);
            var ischinese = charcode >= 19968 && charcode <= 40907;
            chunk = reverse ? char + chunk : chunk + char;
            var charLength = measure(chunk) - chunkLength;
            chunkLength += charLength;
            //var charM = startM + charLength / 2;
            while (offset < end - stride && Math.sqrt(Math.pow(x2 - x0, 2) + Math.pow(y2 - y0, 2)) < charLength / 2) {
                x1 = x2;
                y1 = y2;
                offset += stride;
                x2 = flatCoordinates[offset];
                y2 = flatCoordinates[offset + 1];
            }
            var a = Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);
            var b = 2 * (x2 - x1) * (x1 - x0) + 2 * (y2 - y1) * (y1 - y0);
            var c = Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2) - Math.pow(charLength / 2, 2);
            var scale1 = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
            var scale2 = (-b - Math.sqrt(b * b - 4 * a * c)) / (2 * a);
            interpolate = scale1 < 0 || scale1 > 1 ? scale2 : scale2 < 0 || scale2 > 1 ? scale1 : scale1 < scale2 ? scale2 : scale1;
            var x = _openlayers2.default.math.lerp(x1, x2, interpolate);
            var y = _openlayers2.default.math.lerp(y1, y2, interpolate);

            while (offset < end - stride && Math.sqrt(Math.pow(x2 - x, 2) + Math.pow(y2 - y, 2)) < charLength / 2) {
                x1 = x2;
                y1 = y2;
                offset += stride;
                x2 = flatCoordinates[offset];
                y2 = flatCoordinates[offset + 1];
            }
            a = Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);
            b = 2 * (x2 - x1) * (x1 - x) + 2 * (y2 - y1) * (y1 - y);
            c = Math.pow(x1 - x, 2) + Math.pow(y1 - y, 2) - Math.pow(charLength / 2, 2);
            scale1 = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
            scale2 = (-b - Math.sqrt(b * b - 4 * a * c)) / (2 * a);
            interpolate = scale1 < 0 || scale1 > 1 ? scale2 : scale2 < 0 || scale2 > 1 ? scale1 : scale1 < scale2 ? scale2 : scale1;
            var x3 = _openlayers2.default.math.lerp(x1, x2, interpolate);
            var y3 = _openlayers2.default.math.lerp(y1, y2, interpolate);
            var angle = Math.atan2(y3 - y0, x3 - x0);

            if (reverse) {
                angle += angle > 0 ? -Math.PI : Math.PI;
            }
            if (ischinese && isRotateUp) {
                angle += angle > 0 ? -Math.PI / 2 : Math.PI / 2;
            }
            if (previousAngle !== undefined) {
                var delta = angle - previousAngle;
                delta += delta > Math.PI ? -2 * Math.PI : delta < -Math.PI ? 2 * Math.PI : 0;
                if (ischinese === previousLang ? Math.abs(delta) > maxAngle : Math.abs(delta) > maxAngle + Math.PI / 2) {
                    return null;
                }
            }

            if (previousAngle == angle && !isRotateUp) {
                if (reverse) {
                    data[0] = x;
                    data[1] = y;
                    data[2] = charLength / 2;
                }
                data[4] = chunk;
            } else {
                chunk = char;
                chunkLength = charLength;
                data = [x, y, charLength / 2, angle, chunk];
                if (reverse) {
                    result.unshift(data);
                } else {
                    result.push(data);
                }
                previousAngle = angle;
                previousLang = ischinese;
            }
            x0 = x3;
            y0 = y3;
            startM += charLength;
        }
        return result;
    };
    _openlayers2.default.layer.VectorTile.prototype.setFastRender = function (fastRender) {
        return this.fastRender = fastRender;
    };

    _openlayers2.default.renderer.canvas.VectorTileLayer.prototype.postCompose = function (context, frameState, layerState) {
        var layer = this.getLayer();
        var declutterReplays = layer.getDeclutter() ? {} : null;
        var source = /** @type {ol.source.VectorTile} */layer.getSource();
        var renderMode = layer.getRenderMode();
        var replayTypes = _openlayers2.default.renderer.canvas.VectorTileLayer.VECTOR_REPLAYS[renderMode];
        var pixelRatio = frameState.pixelRatio;
        var rotation = frameState.viewState.rotation;
        var size = frameState.size;
        var offsetX, offsetY;
        if (rotation) {
            offsetX = Math.round(pixelRatio * size[0] / 2);
            offsetY = Math.round(pixelRatio * size[1] / 2);
            _openlayers2.default.render.canvas.rotateAtOffset(context, -rotation, offsetX, offsetY);
        }
        if (declutterReplays) {
            this.declutterTree_.clear();
        }
        var tiles = this.renderedTiles;
        var tileGrid = source.getTileGridForProjection(frameState.viewState.projection);
        var clips = [];
        var zs = [];
        for (var i = tiles.length - 1; i >= 0; --i) {
            var tile = /** @type {ol.VectorImageTile} */tiles[i];
            if (tile.getState() == _openlayers2.default.TileState.ABORT) {
                continue;
            }
            var tileCoord = tile.tileCoord;
            var worldOffset = tileGrid.getTileCoordExtent(tileCoord)[0] - tileGrid.getTileCoordExtent(tile.wrappedTileCoord)[0];
            var transform = undefined;
            for (var t = 0, tt = tile.tileKeys.length; t < tt; ++t) {
                var sourceTile = tile.getTile(tile.tileKeys[t]);
                if (sourceTile.getState() == _openlayers2.default.TileState.ERROR) {
                    continue;
                }
                var replayGroup = sourceTile.getReplayGroup(layer, tileCoord.toString());
                if (renderMode != _openlayers2.default.layer.VectorTileRenderType.VECTOR && (!replayGroup || !replayGroup.hasReplays(replayTypes))) {
                    if (layer.fastRender === true) {
                        sourceTile.replayGroups_ = {};
                        sourceTile.features_ = [];
                    }
                    continue;
                }
                if (!transform) {
                    transform = this.getTransform(frameState, worldOffset);
                }
                var currentZ = sourceTile.tileCoord[0];
                var currentClip = replayGroup.getClipCoords(transform);
                context.save();
                context.globalAlpha = layerState.opacity;
                // Create a clip mask for regions in this low resolution tile that are
                // already filled by a higher resolution tile
                for (var j = 0, jj = clips.length; j < jj; ++j) {
                    var clip = clips[j];
                    if (currentZ < zs[j]) {
                        context.beginPath();
                        // counter-clockwise (outer ring) for current tile
                        context.moveTo(currentClip[0], currentClip[1]);
                        context.lineTo(currentClip[2], currentClip[3]);
                        context.lineTo(currentClip[4], currentClip[5]);
                        context.lineTo(currentClip[6], currentClip[7]);
                        // clockwise (inner ring) for higher resolution tile
                        context.moveTo(clip[6], clip[7]);
                        context.lineTo(clip[4], clip[5]);
                        context.lineTo(clip[2], clip[3]);
                        context.lineTo(clip[0], clip[1]);
                        context.clip();
                    }
                }
                replayGroup.replay(context, transform, rotation, {}, replayTypes, declutterReplays);
                context.restore();
                clips.push(currentClip);
                zs.push(currentZ);
            }
        }
        if (declutterReplays) {
            _openlayers2.default.render.canvas.ReplayGroup.replayDeclutter(declutterReplays, context, rotation);
        }
        if (rotation) {
            _openlayers2.default.render.canvas.rotateAtOffset(context, rotation,
            /** @type {number} */
            offsetX, /** @type {number} */offsetY);
        }
        _openlayers2.default.renderer.canvas.TileLayer.prototype.postCompose.apply(this, arguments);
    };
};
window.olExtends = olExtends;

/***/ })
/******/ ]);