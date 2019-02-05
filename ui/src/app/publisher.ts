export interface Volume {
  volume: string;
  thumbnail: string;
}

export interface Series {
  series: string;
  volumes: Volume[];
}

export interface Publisher {
  publisher: string;
  series: Series[];
}
