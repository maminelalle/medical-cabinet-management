export interface CatalogueActe {
  id: number;
  libelle: string;
  type: string;
  montantDefaut: number;
  actif: boolean;
}

export interface CatalogueActeRequest {
  libelle: string;
  type: string;
  montantDefaut: number;
}