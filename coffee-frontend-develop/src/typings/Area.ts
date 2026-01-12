export type TArea = {
    id: number;
    areaName: string;
    latitude: number;
    longitude: number;
};

export type TSection = {
    id: number;
    sectionName: string;
    latitude: number;
    longitude: number;
    areaName: string;
};

export type TAreaWithSections = TArea & {
    sections: TSection[];
};

// export type TCreateAreaForm = Omit<TArea, 'id'>;
