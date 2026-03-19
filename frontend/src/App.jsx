import React from "react";

import './App.css'
import Tile from './components/Tile/Tile.jsx';

const tiles = [];

let zoom = 19;

function createTiles() {
  for (let y = 0; y < 4; y++) {
    for (let x = 0; x < 7; x++) {
      tiles.push(<Tile x={x} y={y} zoom={zoom} />);
    }
  }
}

function App() {
  createTiles();

  return (
      <div className="map">
        {tiles}
      </div>
  );
}

export default App
