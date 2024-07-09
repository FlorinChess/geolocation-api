# Responses samples

## Notes

* `name` can be extracted from the key-value pairs of the tags
* `length` can be calculatad from the geometry
* `geom` is the GeoJSON of the loaded geometry (A0: copy the response from one of the samples, the easiest is a point, A1: JTS has this functionality built-in)
* `tags` is just a map from String to String

## Amenities

`GET http://localhost:8010/amenities?point.x=15.44679&point.y=47.06646&point.d=100&take=2&amenity=restaurant`

```json
{
	"entries": [
		{
			"name": "Athen",
			"id": 291464594,
			"geom": {
				"type": "Point",
				"coordinates": [
					15.446455,
					47.066807
				],
				"crs": {
					"type": "name",
					"properties": {
						"name": "EPSG:0"
					}
				}
			},
			"tags": {
				"note": "Tür zum Raucherbereich stand ständig offen, deshalb 'separated'.",
				"wheelchair": "no",
				"amenity": "restaurant",
				"addr:country": "AT",
				"check_date:opening_hours": "2023-04-26",
				"cuisine": "greek",
				"contact:email": "restaurant.athen1992@gmail.com",
				"addr:postcode": "8010",
				"addr:city": "Graz",
				"diet:vegetarian": "yes",
				"addr:housenumber": "9",
				"contact:phone": "+43 316 816111",
				"indoor_seating": "yes",
				"smoking": "separated",
				"name": "Athen",
				"opening_hours": "Mo-Sa 11:00-24:00; PH,Su 17:00-24:00",
				"addr:street": "Schlögelgasse",
				"outdoor_seating": "no",
				"contact:website": "https://restaurant-athen-graz.eatbu.com/"
			},
			"type": "restaurant"
		},
		{
			"name": "Dim Sum",
			"id": 1618928383,
			"geom": {
				"type": "Point",
				"coordinates": [
					15.447004,
					47.06662
				],
				"crs": {
					"type": "name",
					"properties": {
						"name": "EPSG:0"
					}
				}
			},
			"tags": {
				"amenity": "restaurant",
				"addr:country": "AT",
				"cuisine": "asian",
				"addr:postcode": "8010",
				"takeaway": "yes",
				"addr:city": "Graz",
				"addr:housenumber": "2",
				"indoor_seating": "yes",
				"smoking": "separated",
				"name": "Dim Sum",
				"opening_hours": "Tu-Fr 11:00-15:00,17:00-22:00; Sa,Su 11:00-22:00",
				"addr:street": "Dietrichsteinplatz",
				"outdoor_seating": "no"
			},
			"type": "restaurant"
		}
	],
	"paging": {
		"skip": 0,
		"take": 2,
		"total": 3
	}
}
```

## Amenity by ID

`GET  http://localhost:8010/amenities/1765378024`

```json
{
	"name": "",
	"id": 1765378024,
	"geom": {
		"type": "Point",
		"coordinates": [
			15.466168,
			47.055058
		],
		"crs": {
			"type": "name",
			"properties": {
				"name": "EPSG:0"
			}
		}
	},
	"tags": {
		"amenity": "bicycle_parking",
		"bicycle_parking": "stands",
		"covered": "yes",
		"source": "survey",
		"capacity": "38"
	},
	"type": "bicycle_parking"
}
```

## Roads

`GET http://localhost:8010/roads?bbox.tl.x=15.45534&bbox.tl.y=47.05938&bbox.br.x=15.46127&bbox.br.y=47.05709&road=residential&take=1`

```json
{
  "entries": [
    {
      "name": "Sandgasse",
      "id": 32685265,
      "geom": {
        "type": "LineString",
        "coordinates": [
          [
            15.45578,
            47.058266
          ],
          [
            15.456056,
            47.058384
          ],
          [
            15.456229,
            47.058456
          ]
        ],
        "crs": {
          "type": "name",
          "properties": {
            "name": "EPSG:0"
          }
        }
      },
      "tags": {
        "sidewalk": "separate",
        "surface": "asphalt",
        "lit": "yes",
        "maxspeed": "30",
        "name": "Sandgasse",
        "width": "6.5",
        "parking:lane:right": "parallel",
        "highway": "residential"
      },
      "type": "residential",
      "child_ids": [
        21099615,
        20832686,
        361348254
      ]
    }
  ],
  "paging": {
    "skip": 0,
    "take": 1,
    "total": 6
  }
}
```

## Road By ID

`GET http://localhost:8010/roads/5113075`

```json
{
  "name": "Süd Autobahn",
  "id": 5113075,
  "geom": {
    "type": "LineString",
    "coordinates": [
      [
        15.473895,
        47.016022
      ],
      [
        15.473071,
        47.015839
      ]
    ],
    "crs": {
      "type": "name",
      "properties": {
        "name": "EPSG:0"
      }
    }
  },
  "tags": {
    "note": "maxspeed is shown on matrix display, but fixed always 100 kmh",
    "surface": "asphalt",
    "maxspeed": "100",
    "source:maxspeed": "VO IG-L Feldkirchen https://www.ris.bka.gv.at/GeltendeFassung.wxe?Abfrage=LrStmk&Gesetzesnummer=20001351",
    "layer": "3",
    "oneway": "yes",
    "int_ref": "E 59;E 66",
    "ref": "A2",
    "lanes": "3",
    "name": "Süd Autobahn",
    "bridge": "yes",
    "placement": "right_of:1",
    "highway": "motorway",
    "maxspeed:variable": "no"
  },
  "type": "motorway",
  "child_ids": [
    35136721,
    20949405
  ]
}
```

## Routing

`GET http://localhost:8010/route?from=20929584&to=6137492613&weighting=time`

```json
{
	"length": 2480.1975,
	"time": 4.262986,
	"roads": [
		{
			"name": "Inffeldgasse",
			"id": 28790006,
			"geom": {
				"type": "LineString",
				"coordinates": [
					[
						15.463948,
						47.059845
					],
					[
						15.463986,
						47.05986
					],
					[
						15.464215,
						47.059948
					],
					[
						15.464289,
						47.059986
					]
				],
				"crs": {
					"type": "name",
					"properties": {
						"name": "EPSG:0"
					}
				}
			},
			"tags": {
				"sidewalk": "both",
				"sidewalk:both:surface": "asphalt",
				"surface": "asphalt",
				"lit": "yes",
				"lanes": "2",
				"maxspeed": "30",
				"name": "Inffeldgasse",
				"highway": "service"
			},
			"type": "service",
			"child_ids": [
				6137492613,
				21298441,
				6137492615,
				1224656471
			]
		},
		//...
	]
}

```

## Landuse

`GET http://localhost:8010/usage?bbox.tl.x=15.44967525394921&bbox.tl.y=47.07030450952097&bbox.br.x=15.466605357251211&bbox.br.y=47.06556178619343`

```json
{
    "area": 1261035.1,
    "usages": [
        {
            "type": "garages",
            "share": 9.4853994E-4,
            "area": 1196.1422
        },
        {
            "type": "commercial",
            "share": 0.0011187064,
            "area": 1410.728
        },
        {
            "type": "greenfield",
            "share": 0.0020913205,
            "area": 2637.2285
        },
        {
            "type": "construction",
            "share": 0.0048213657,
            "area": 6079.9116
        },
        {
            "type": "grass",
            "share": 0.0048657754,
            "area": 6135.9136
        },
        {
            "type": "farmland",
            "share": 0.0060854903,
            "area": 7674.017
        },
        {
            "type": "forest",
            "share": 0.011818454,
            "area": 14903.485
        },
        {
            "type": "education",
            "share": 0.028418193,
            "area": 35836.34
        }
    ]
}
```


## Errors

Any error you manually create should have the following very simple schema with a message of your choice (just return something that makes sense in this situation, for easiere debugability!)

```json
{
	"message": "Did not find requested entity!"
}
```