import React from 'react';
import { IconsProvider } from '@talend/react-components';
import { Notification, ShortcutManager } from '@talend/react-containers';

// import { I18nextProvider } from 'react-i18next';
// import i18n from '@talend/data-quality-semantic-ee/lib/app/i18n';

import AboutModal from './AboutModal';
import PreparationCreatorModal from '../../components/preparation-creator/index';

export default function App(props) {
	/**
	 * Instantiate all global components here
	 * Ex : we register @talend/react-components <IconsProvider />
	 * so that all icons are available in each view
	 */
	return (
		// <I18nextProvider i18n={i18n}>
		<div>
			<IconsProvider />
			<ShortcutManager view="shortcuts" />
			<Notification />
			<AboutModal />
			<PreparationCreatorModal />
			{props.children}
		</div>
		// </I18nextProvider>
	);
}

App.displayName = 'App';
App.propTypes = {
	children: React.PropTypes.element.isRequired,
};
