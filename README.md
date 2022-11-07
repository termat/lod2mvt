# *lod2mvt*
## これは何ですか？
- CityGML／Geojsonからベクトルタイルを生成するツールです。
- CityGMLの解釈は[citygml4j](https://github.com/citygml4j)を使用してます。
- ベクトルタイルの生成は[MapBox Vector Tile - Java](https://github.com/wdtinc/mapbox-vector-tile-java)を使用してます。
- LOD2モデルのベクトルタイル化はindigo-labさんの[plateau-lod2-mvt](https://github.com/indigo-lab/plateau-lod2-mvt)の方法を用いています。
- なお、CityGMLはi-UR1.5に改定に適合している必要があります。

## 概要
### LOD2MVT
- CityGMLからベクトルタイルを生成
- 入力先のCityGMLが入ったフォルダを指定
- 出力先にベクトルタイルの出力先を指定
- ベクトルタイルを生成する最小/最大ズームレベルを指定
- 実行ボタンでCityGMLを逐次処理し、ベクトルタイルを生成
- BuildingデータはベクトルタイルのBUILDINGレイヤー、BridgeデータはBRIDGEレイヤー、RoadデータはROADレイヤーに出力されます。

![LOD2MVT](/images/lod2mvy.png)

### Geojson2Mvt
- Geojsonからベクトルタイルを生成
- データは設定した「レイヤー名」で出力されます。

![LOD2MVT](/images/geojson2mvt.png)


