import React from 'react';

import { DialogProps } from './Dialog';

export const DialogContext = React.createContext<{
    openDialog: (props: Omit<DialogProps, 'open' | 'onClose'>) => void;
}>({
    openDialog: () => {},
});
